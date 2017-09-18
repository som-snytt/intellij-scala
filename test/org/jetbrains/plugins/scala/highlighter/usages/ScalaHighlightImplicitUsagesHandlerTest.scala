package org.jetbrains.plugins.scala.highlighter.usages

import com.intellij.codeInsight.highlighting.{HighlightUsagesHandler, HighlightUsagesHandlerBase}
import com.intellij.psi.PsiElement
import com.intellij.testFramework.EditorTestUtil.{CARET_TAG => CARET}
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter
import org.junit.Assert

/**
  * Created by Ignat Loskutov on 10.07.17.
  */
class ScalaHighlightImplicitUsagesHandlerTest extends ScalaLightCodeInsightFixtureTestAdapter {

  def testNoUsages(): Unit = {
    val code =
      s"""
         |object AAA {
         |  def foo(i: Int): Int = i
         |  implicit val implicit${CARET}Int = 0
         |}
      """.stripMargin
    doTest(code, Seq.empty)
  }

  def testImplicitParameter(): Unit = {
    val code =
      s"""
         |object AAA {
         |  def foo()(implicit i: Int): Int = i
         |  implicit val impl${CARET}icitInt = 0
         |  foo()
         |}
      """.stripMargin
    doTest(code, Seq("foo()"))
  }

  def testImplicitConversion(): Unit = {
    val code =
      s"""
         |object AAA {
         |  implicit def stri${CARET}ngToInt(s: String): Int = Integer.parseInt(s)
         |  def inc(i: Int): Int = i + 1
         |  inc("123")
         |}
      """.stripMargin
    doTest(code, Seq("\"123\""))
  }

  def testBothParameterAndConversion(): Unit = {
    val code =
      s"""
         |object AAA {
         |  implicit def stri${CARET}ngToInt(s: String): Int = Integer.parseInt(s)
         |  def inc(i: Int): Int = i + 1
         |  def foo(s: String)(implicit converter: String => Int): Int = converter(s)
         |  inc("123")
         |  foo("42")
         |}
      """.stripMargin
    doTest(code, Seq("\"123\"", "foo(\"42\")"))
  }

  def testHighlightedRangesAreCorrect(): Unit = {
    val code =
      s"""
         |object AAA {
         |  implicit val theAn${CARET}swer: Int = 42
         |  def increase(i: Int)(implicit summand: Int): Int = i + summand
         |  increase(0)
         |  this.increase(0)
         |  (this.increase)(0)
         |  increase       (0)
         |}
      """.stripMargin
    doTest(
      code,
      Seq(
        "increase(0)",
        "increase(0)",
        "(this.increase)(0)",
        "increase       (0)"
      )
    )
  }

  def testApply(): Unit = {
    val code =
      s"""
         |object AAA {
         |  implicit val theAn${CARET}swer: Int = 42
         |  def apply(i: Int)(implicit suffix: Int): Int = i + suffix
         |  this(0)
         |}
      """.stripMargin
    doTest(code, Seq("this(0)"))
  }

  def testContextBounds(): Unit = {
    val code =
      s"""
         |trait Semigroup[T] {
         |  def op(a: T, b: T): T
         |}
         |
         |implicit val intSem${CARET}igroup: Semigroup[Int] = (a: Int, b: Int) => a + b
         |
         |def double[T : Semigroup](t: T) = implicitly[Semigroup[T]].op(t, t)
         |
         |double(1)
      """.stripMargin
    doTest(code, Seq("double(1)"))
  }

  def testContextBoundsColon(): Unit = {
    val code =
      s"""
         |trait Semigroup[T] {
         |  def op(a: T, b: T): T
         |}
         |
         |implicit val intSemigroup: Semigroup[Int] = (a: Int, b: Int) => a + b
         |
         |def double[T $CARET: Semigroup](t: T) = implicitly[Semigroup[T]].op(t, t)
      """.stripMargin
    doTest(code, Seq("implicitly[Semigroup[T]]"))
  }

  def testContextBoundsColon2(): Unit = {
    val code =
      s"""
         |trait Semigroup[T] {
         |  def op(a: T, b: T): T
         |}
         |
         |implicit val intSemigroup: Semigroup[Int] = (a: Int, b: Int) => a + b
         |
         |def double[T:$CARET Semigroup](t: T) = implicitly[Semigroup[T]].op(t, t)
      """.stripMargin
    doTest(code, Seq("implicitly[Semigroup[T]]"))
  }

  def testImplicitClass(): Unit = {
    val code =
      s"""
         |object Test {
         |  implicit class ${CARET}StringExt(val s: String) {
         |    def twice: String = s + s
         |  }
         |  val string = "a"
         |  string.twice
         |}
       """.stripMargin
    doTest(code, Seq("string"))
  }

  def testConstructorParameter(): Unit = {
    val code =
      s"""
         |object Test {
         |  implicit val ${CARET}theAnswer: Int = 42
         |
         |  class AB(i: Int)(implicit j: Int) {
         |    def this(ints: Array[Int])(implicit j: Int) = this(ints(0))
         |  }
         |
         |  new AB(1)
         |  new AB(Array(1))
         |}
       """.stripMargin
    doTest(code, Seq("new AB(1)", "new AB(Array(1))"))
  }

  def testTypeClasses1(): Unit = {
    val code =
      s"""
         |object Test {
         |  trait Ordering[T]
         |
         |  implicit def seqDerivedOrdering[CC[X] <: scala.collection.Seq[X], T](implicit ord: Ordering[T]): Ordering[CC[T]] = ???
         |  implicit def tuple2Ordering[T1, T2](implicit ord1: Ordering[T1], ord2: Ordering[T2]): Ordering[(T1, T2)] = ???
         |
         |  implicit object ${CARET}BooleanOrdering extends Ordering[Boolean]
         |  implicit object IntOrdering extends Ordering[Int]
         |
         |  def sort[T](t: Seq[T])(implicit ordering: Ordering[T]) = ???
         |
         |  sort(Seq((Seq(12), (true, Seq(false)))))
         |  sort(Seq((Seq(12), false)))
         |  sort(Seq(false))
         |  sort(Seq((Seq(12), (1, Seq(2)))))
         |}
        """.stripMargin

    doTest(code, Seq(
      "sort(Seq((Seq(12), (true, Seq(false)))))",
      "sort(Seq((Seq(12), false)))",
      "sort(Seq(false))"))
  }

  def testTypeClasses2(): Unit = {
    val code =
      s"""
         |object Test {
         |  trait Ordering[T]
         |
         |  implicit def seqDerivedOrdering[CC[X] <: scala.collection.Seq[X], T](implicit ord: Ordering[T]): Ordering[CC[T]] = ???
         |  implicit def ${CARET}tuple2Ordering[T1, T2](implicit ord1: Ordering[T1], ord2: Ordering[T2]): Ordering[(T1, T2)] = ???
         |
         |  implicit object BooleanOrdering extends Ordering[Boolean]
         |  implicit object IntOrdering extends Ordering[Int]
         |
         |  def sort[T](t: Seq[T])(implicit ordering: Ordering[T]) = ???
         |
         |  sort(Seq((Seq(12), (true, Seq(false)))))
         |  sort(Seq((Seq(12), false)))
         |  sort(Seq(false))
         |  sort(Seq((Seq(12), (1, Seq(2)))))
         |}
        """.stripMargin

    doTest(code, Seq(
      "sort(Seq((Seq(12), (true, Seq(false)))))",
      "sort(Seq((Seq(12), false)))",
      "sort(Seq((Seq(12), (1, Seq(2)))))"))
  }


  def doTest(fileText: String, expected: Seq[String]): Unit = {
    import scala.collection.JavaConversions._
    myFixture.configureByText("dummy.scala", fileText)
    val handler = createHandler
    val targets = handler.getTargets
    Assert.assertEquals(1, targets.size())
    handler.computeUsages(targets)
    val actualUsages: Seq[String] = handler.getReadUsages.map(_.substring(getFile.getText))
    Assert.assertEquals(s"actual: $actualUsages, expected: $expected", expected, actualUsages)
  }

  def createHandler: HighlightUsagesHandlerBase[PsiElement] =
    HighlightUsagesHandler.createCustomHandler(getEditor, getFile).asInstanceOf[HighlightUsagesHandlerBase[PsiElement]]

}
