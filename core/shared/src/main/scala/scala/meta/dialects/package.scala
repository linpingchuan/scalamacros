package scala.meta

import scala.language.experimental.macros
import scala.meta.internal.dialects._
import scala.meta.dialects._
import scala.meta.prettyprinters._
import scala.meta.internal.prettyprinters._

// NOTE: Can't put Dialect into scala.meta.dialects,
// because then implicit scope for Dialect lookups will contain members of the package object,
// e.g. both Scala211 and Dotty, which is definitely not what we want.
final case class Dialect(
    // Are `&` intersection types supported by this dialect?
    allowAndTypes: Boolean,
    // Are extractor varargs specified using ats, i.e. is `case Extractor(xs @ _*)` legal or not?
    allowAtForExtractorVarargs: Boolean,
    // Are extractor varargs specified using colons, i.e. is `case Extractor(xs: _*)` legal or not?
    allowColonForExtractorVarargs: Boolean,
    // Are `inline` identifiers supported by this dialect?
    allowInlineIdents: Boolean,
    // Are inline vals and defs supported by this dialect?
    allowInlineMods: Boolean,
    // Are literal types allowed, i.e. is `val a : 42 = 42` legal or not?
    allowLiteralTypes: Boolean,
    // Are multiline programs allowed?
    // Some quasiquotes only support single-line snippets.
    allowMultilinePrograms: Boolean,
    // Are `|` (union types) supported by this dialect?
    allowOrTypes: Boolean,
    // Are unquotes ($x) and splices (..$xs, ...$xss) allowed?
    // If yes, they will be parsed as patterns.
    allowPatUnquotes: Boolean,
    // Are naked underscores allowed after $ in pattern interpolators, i.e. is `case q"$_ + $_" =>` legal or not?
    allowSpliceUnderscores: Boolean,
    // Are unquotes ($x) and splices (..$xs, ...$xss) allowed?
    // If yes, they will be parsed as terms.
    allowTermUnquotes: Boolean,
    // Are terms on the top level supported by this dialect?
    // Necessary to support popular script-like DSLs.
    allowToplevelTerms: Boolean,
    // Are trailing commas allowed? SIP-27.
    allowTrailingCommas: Boolean,
    // Are trait allowed to have parameters?
    // They are in Dotty, but not in Scala 2.12 or older.
    allowTraitParameters: Boolean,
    // Are view bounds supported by this dialect?
    // Removed in Dotty.
    allowViewBounds: Boolean,
    // Are `with` intersection types supported by this dialect?
    allowWithTypes: Boolean,
    // Are XML literals supported by this dialect?
    // We plan to deprecate XML literal syntax, and some dialects
    // might go ahead and drop support completely.
    allowXmlLiterals: Boolean,
    // What kind of separator is necessary to split top-level statements?
    // Normally none is required, but scripts may have their own rules.
    toplevelSeparator: String
) extends Pretty {
  // Are unquotes ($x) and splices (..$xs, ...$xss) allowed?
  def allowUnquotes: Boolean = allowTermUnquotes || allowPatUnquotes

  // Dialects have reference equality semantics,
  // because sometimes dialects representing distinct Scala versions
  // can be structurally equal to each other.
  override def canEqual(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]
  override def equals(other: Any): Boolean = this eq other.asInstanceOf[AnyRef]
  override def hashCode: Int = System.identityHashCode(this)

  override protected def syntax(p: Prettyprinter): Unit = structure(p)
  override protected def structure(p: Prettyprinter): Unit = {
    val renderStandardName = Dialect.standards.get(this).map(p.raw)
    renderStandardName.getOrElse(super.structure(p))
  }
}

object Dialect extends InternalDialect {
  // NOTE: Spinning up a macro just for this is too hard.
  // Using JVM reflection won't be portable to Scala.js.
  @volatile private[meta] lazy val standards: Map[Dialect, String] = Map(
    Scala210 -> "Scala210",
    Sbt0136 -> "Sbt0136",
    Sbt0137 -> "Sbt0137",
    Scala211 -> "Scala211",
    Typelevel211 -> "Typelevel211",
    Paradise211 -> "Paradise211",
    ParadiseTypelevel211 -> "ParadiseTypelevel211",
    Scala212 -> "Scala212",
    Typelevel212 -> "Typelevel212",
    Paradise212 -> "Paradise212",
    ParadiseTypelevel212 -> "ParadiseTypelevel212",
    Scala213 -> "Scala213",
    Dotty -> "Dotty"
  )
}

package object dialects {
  implicit val Scala210: Dialect = Dialect(
    allowAndTypes = false,
    allowAtForExtractorVarargs = true,
    allowColonForExtractorVarargs = false,
    allowInlineIdents = true,
    allowInlineMods = false,
    allowLiteralTypes = false,
    allowMultilinePrograms = true,
    allowOrTypes = false,
    allowPatUnquotes = false,
    allowSpliceUnderscores = false, // SI-7715, only fixed in 2.11.0-M5
    allowTermUnquotes = false,
    allowToplevelTerms = false,
    allowTrailingCommas = false,
    allowTraitParameters = false,
    allowViewBounds = true,
    allowWithTypes = true,
    allowXmlLiterals = true, // Not even deprecated yet, so we need to support xml literals
    toplevelSeparator = ""
  )

  implicit val Sbt0136: Dialect = Scala210.copy(
    allowToplevelTerms = true,
    toplevelSeparator = EOL
  )

  implicit val Sbt0137: Dialect = Scala210.copy(
    allowToplevelTerms = true,
    toplevelSeparator = ""
  )

  implicit val Scala211: Dialect = Scala210.copy(
    allowSpliceUnderscores = true // SI-7715, only fixed in 2.11.0-M5
  )

  implicit val Typelevel211: Dialect = Scala211.copy(
    allowLiteralTypes = true
  )

  implicit val Paradise211: Dialect = Scala211.copy(
    allowInlineIdents = true,
    allowInlineMods = true
  )

  implicit val ParadiseTypelevel211: Dialect = Typelevel211.copy(
    allowInlineIdents = true,
    allowInlineMods = true
  )

  implicit val Scala212: Dialect = Scala211.copy(
    // NOTE: support for literal types is tentatively scheduled for 2.12.3
    // https://github.com/scala/scala/pull/5310#issuecomment-290617202
    allowLiteralTypes = false,
    allowTrailingCommas = true
  )

  implicit val Typelevel212: Dialect = Scala212.copy(
    allowLiteralTypes = true
  )

  implicit val Paradise212: Dialect = Scala212.copy(
    allowInlineIdents = true,
    allowInlineMods = true
  )

  implicit val ParadiseTypelevel212: Dialect = Typelevel212.copy(
    allowInlineIdents = true,
    allowInlineMods = true
  )

  implicit val Scala213: Dialect = Scala212.copy()

  implicit val Dotty: Dialect = Scala211.copy(
    allowAndTypes = true, // New feature in Dotty
    allowAtForExtractorVarargs = false, // New feature in Dotty
    allowColonForExtractorVarargs = true, // New feature in Dotty
    allowInlineIdents = false, // New feature in Dotty
    allowInlineMods = true, // New feature in Dotty
    allowLiteralTypes = true, // New feature in Dotty
    allowOrTypes = true, // New feature in Dotty
    allowTrailingCommas = false, // Not yet implemented in Dotty
    allowTraitParameters = true, // New feature in Dotty
    allowViewBounds = false, // View bounds have been removed in Dotty
    allowWithTypes = false, // New feature in Dotty
    allowXmlLiterals = false // Dotty parser doesn't have the corresponding code, so it can't really support xml literals
  )

  // TODO: https://github.com/scalameta/scalameta/issues/380
  private[meta] def QuasiquoteTerm(underlying: Dialect, multiline: Boolean): Dialect = {
    require(!underlying.allowUnquotes)
    underlying.copy(allowTermUnquotes = true, allowMultilinePrograms = multiline)
  }

  // TODO: https://github.com/scalameta/scalameta/issues/380
  private[meta] def QuasiquotePat(underlying: Dialect, multiline: Boolean): Dialect = {
    require(!underlying.allowUnquotes)
    underlying.copy(allowPatUnquotes = true, allowMultilinePrograms = multiline)
  }
}
