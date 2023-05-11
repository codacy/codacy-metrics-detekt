package codacy.metrics

import java.nio.file.Paths

import com.codacy.plugins.api.languages.{Language, Languages}
import com.codacy.plugins.api.metrics.{FileMetrics, MetricsTool}
import com.codacy.plugins.api.{Options, Source}
import io.github.detekt.metrics.processors._
import io.github.detekt.parser.KtCompiler
import io.github.detekt.tooling.dsl.{ProcessingSpecBuilder, ProjectSpecBuilder}
import io.gitlab.arturbosch.detekt.api._
import io.gitlab.arturbosch.detekt.core.config.YamlConfig
import io.gitlab.arturbosch.detekt.core._
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object Detekt extends MetricsTool {
  private val processors =
    List[FileProcessListener](new ProjectLOCProcessor,
                              new ProjectCLOCProcessor,
                              new ProjectComplexityProcessor,
                              new ClassCountProcessor,
                              new FunctionCountProcessor)

  override def apply(
      source: Source.Directory,
      language: Option[Language],
      files: Option[Set[Source.File]],
      options: Map[Options.Key, Options.Value]): Try[List[FileMetrics]] = {
    for {
      _ <- validateLanguage(language)
      metrics <- computeFileMetrics(source.path, files)
    } yield metrics
  }

  private def validateLanguage(maybeLanguage: Option[Language]) =
    maybeLanguage match {
      case Some(language) if language != Languages.Kotlin =>
        Failure(new Exception(s"Unsupported language detected: $language"))
      case l => Success(l)
    }

  private def computeFileMetrics(directory: String,
                                 filesOpt: Option[Set[Source.File]]) = {
    Try {
      val config = new YamlConfig(new java.util.HashMap(), "")

      val projectSpec = new ProjectSpecBuilder().build()

      val specs = new ProcessingSpecBuilder().build()

      val settings = new ProcessingSettings(specs, config)

      val compiler = new KtTreeCompiler(settings, projectSpec, new KtCompiler())

      val ktFiles = compiler.compile(Paths.get(directory)).asScala

      // set files to be analyzed if they exist
      val filteredKtFiles = filterKtFiles(ktFiles, filesOpt)

      val analyzer =
        new Analyzer(settings, List.empty.asJava, processors.asJava)

      // this will add metrics info to each ktFile
      analyzer.run(filteredKtFiles.asJava, BindingContext.EMPTY)

      filteredKtFiles.map { file =>
        toFileMetrics(file)
      }.toList
    }
  }

  private def filterKtFiles(ktFiles: mutable.Buffer[KtFile],
                            filesOpt: Option[Set[Source.File]]) = {
    filesOpt match {
      case None => ktFiles
      case Some(files) =>
        ktFiles.filter(kt => files.exists(f => kt.getName.endsWith(f.path)))
    }
  }

  private def toFileMetrics(file: KtFile) = {
    FileMetrics(
      filename = file.getName,
      complexity = Option(
        file
          .getUserData(ProjectComplexityProcessorKt.getComplexityKey)
          .intValue()),
      loc =
        Option(file.getUserData(ProjectLOCProcessorKt.getLinesKey).intValue()),
      cloc = Option(
        file.getUserData(ProjectCLOCProcessorKt.getCommentLinesKey).intValue()),
      nrMethods = Option(
        file
          .getUserData(FunctionCountProcessorKt.getNumberOfFunctionsKey)
          .intValue()),
      nrClasses = Option(
        file
          .getUserData(ClassCountProcessorKt.getNumberOfClassesKey)
          .intValue()),
      lineComplexities = Set.empty
    )
  }
}
