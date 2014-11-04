package scalatex


import org.parboiled2._
import torimatomeru.ScalaSyntax

import scalatex.stages.{Parser, Ast}
import Ast.Block.Text
import Ast.Chain.Args

object ParserTests extends utest.TestSuite{
  import Ast._
  import utest._
  def check[T](input: String, parse: Parser => scala.util.Try[T], expected: T) = {
    val parsed = parse(new Parser(input))
    assert(parsed.get == expected)
  }
  def tests = TestSuite{
    'Trim{
      def wrap(s: String) = "|" + s + "|"
      * - {
        val trimmed = wrap(stages.Trim("""
            i am cow
              hear me moo
                i weigh twice as much as you
        """))
        val expected = wrap("""
                         |i am cow
                         |  hear me moo
                         |    i weigh twice as much as you
                         |""".stripMargin)
         assert(trimmed == expected)

      }
      * - {
        val trimmed = wrap(stages.Trim(
          """
          @{"lol" * 3}
          @{
            val omg = "omg"
            omg * 2
          }
          """
        ))
        val expected = wrap(
          """
            |@{"lol" * 3}
            |@{
            |  val omg = "omg"
            |  omg * 2
            |}
            |""".stripMargin
        )
        assert(trimmed == expected)
      }
      'dropTrailingWhitespace - {

        val trimmed = wrap(stages.Trim(
          Seq(
            "  i am a cow   ",
            "    hear me moo    ",
            "   i weigh twice as much as you"
          ).mkString("\n")
        ))
        val expected = wrap(
          Seq(
            "i am a cow",
            "  hear me moo",
            " i weigh twice as much as you"
          ).mkString("\n")
        )
        assert(trimmed == expected)
      }
    }
    'Text {
      * - check("i am a cow", _.Text.run(), Block.Text("i am a cow"))
      * - check("i am a @cow", _.Text.run(), Block.Text("i am a "))
      * - check("i am a @@cow", _.Text.run(), Block.Text("i am a @cow"))
      * - check("i am a @@@cow", _.Text.run(), Block.Text("i am a @"))
      * - check("i am a @@@@cow", _.Text.run(), Block.Text("i am a @@cow"))

    }
    'Code{
      'identifier - check("@gggg  ", _.Code.run(), "gggg")
      'parens - check("@(1 + 1)lolsss\n", _.Code.run(),  "(1 + 1)")
      'curlies - check("@{{1} + (1)}  ", _.Code.run(), "{{1} + (1)}")
      'blocks - check("@{val x = 1; 1} ", _.Code.run(), "{val x = 1; 1}")
      'weirdBackticks - check("@{`{}}{()@`}\n", _.Code.run(), "{`{}}{()@`}")
     }
    'MiscCode{
      'imports{
        * - check("@import math.abs", _.Header.run(), "import math.abs")
        * - check("@import math.{abs, sin}", _.Header.run(), "import math.{abs, sin}")
      }
      'headerblocks{
        check(
          """@import math.abs
            |@import math.sin
            |
            |hello world
            |""".stripMargin,
          _.HeaderBlock.run(),
          Ast.Block(
            Some("import math.abs\nimport math.sin"),
            Seq(Text("\n"), Text("hello world"), Text("\n"))
          )
        )
      }

    }
    'Block{
      * - check("{i am a cow}", _.TBlock.run(), Block(None, Seq(Block.Text("i am a cow"))))
      * - check("{i @am a @cow}", _.TBlock.run(),
        Block(None, Seq(
          Block.Text("i "),
          Chain("am",Seq()),
          Block.Text(" a "),
          Chain("cow",Seq())
        ))
      )
    }
    'Chain{
      * - check("@omg.bbq[omg].fff[fff](123)  ", _.ScalaChain.run(),
        Chain("omg",Seq(
          Chain.Prop("bbq"),
          Chain.TypeArgs("[omg]"),
          Chain.Prop("fff"),
          Chain.TypeArgs("[fff]"),
          Chain.Args("(123)")
        ))
      )
      * - check("@omg{bbq}.cow(moo){a @b}\n", _.ScalaChain.run(),
        Chain("omg",Seq(
          Block(None, Seq(Block.Text("bbq"))),
          Chain.Prop("cow"),
          Chain.Args("(moo)"),
          Block(None, Seq(Block.Text("a "), Chain("b", Nil)))
        ))
      )
    }
    'Body{
      'indents - check(
        """
          |@omg
          |  @wtf
          |    @bbq
          |      @lol""".stripMargin,
        _.Body.run(),
        Block(None, Seq(
          Text("\n"),
          Chain("omg",Seq(Block(None, Seq(
            Text("\n  "),
            Chain("wtf",Seq(Block(None, Seq(
              Text("\n    "),
              Chain("bbq",Seq(Block(None, Seq(
                Text("\n      "),
                Chain("lol",Seq())
              ))))
            ))))
          ))))
        ))
      )
      'dedents - check(
        """
          |@omg
          |  @wtf
          |@bbq""".stripMargin,
        _.Body.run(),
        Block(None, Seq(
          Text("\n"),
          Chain("omg",Seq(Block(
            None,
            Seq(
              Text("\n  "),
              Chain("wtf",Seq()))
          ))),
          Text("\n"),
          Chain("bbq", Seq())
        ))
      )
      'dedentText - check(
        """
          |@omg("lol", 1, 2)
          |  @wtf
          |bbq""".stripMargin,
        _.Body.run(),
        Block(None, Seq(
          Text("\n"),
          Chain("omg",Seq(
            Args("""("lol", 1, 2)"""),
            Block(None, Seq(
              Text("\n  "),
              Chain("wtf",Seq())))
          )),
          Text("\n"),
          Text("bbq")
        ))
      )
//      * - check(
//        """
//          |@omg("lol",
//          |1,
//          |       2
//          |    )
//          |  wtf
//          |bbq""".stripMargin,
//        _.Body.run(),
//        Block(Seq(
//          Chain("omg",Seq(
//            Args("(\"lol\",\n1,\n       2\n    )"),
//            Block(Seq(
//              Text("\n  "), Text("wtf")
//            ))
//          )),
//          Text("\n"),
//          Text("bbq")
//        ))
//      )
      'codeBlocks - check(
        """
          |@{"lol" * 3}
          |@{
          |  val omg = "omg"
          |  omg * 2
          |}""".stripMargin,
        _.Body.run(),
        Block(None, Seq(
          Text("\n"),
          Chain("{\"lol\" * 3}", Seq()),
          Text("\n"),
          Chain("""{
            |  val omg = "omg"
            |  omg * 2
            |}""".stripMargin,
            Seq()
          )
        ))
      )
    }
  }
}


