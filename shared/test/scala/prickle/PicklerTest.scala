package prickle

import utest._
import scala.util.Success

import scala.collection.mutable


abstract class PickleTests[P](configFactory: Boolean => PConfig[P], testData: TestData[P]) extends TestSuite {
  import testData._


  val tests = TestSuite {
    "with00cyclic00config"-{
      implicit val cyclicConfig = configFactory(true)

      "caseclass"-{
        "encoding"-{
          val actual = Pickle(benDetails)
          assert(expectedBenDetailsPickle == actual)
        }
        "unpickling"-{
          val actual = Unpickle[PersonalDetails].from(expectedBenDetailsPickle)
          assert(Success(benDetails) == actual)
        }
        "toleratesextradata"-{
          val extra = addField(expectedBenDetailsPickle, "foo" -> cyclicConfig.makeString("bar"))

          val actual = Unpickle[PersonalDetails].from(extra).get
          assert(benDetails == actual)
        }
      }
      "generic"-{
        val initial: (Person, Person) = (ben, parent)
        val p = Pickle(initial)
        val unpickled = Unpickle[(Person, Person)].from(p).get
        assert(initial == unpickled)
      }
      "compositepicklers"-{
        "apple"-{
          "pickle"-{
            val applePickle = Pickle(apple)
            assert(expectedApplePickle == applePickle)
            val plantPickle = Pickle(apple: EdiblePlant)
            assert(plantPickle == applePickle)
          }
          "unpickle"-{
            val unpickleFruit = Unpickle[Fruit].from(expectedApplePickle).get
            assert(apple == unpickleFruit)
            val unpicklePlant = Unpickle[EdiblePlant].from(expectedApplePickle).get
            assert(apple == unpicklePlant)
          }
        }
        "carrot"-{
          "pickle"-{
            val carrotPickle = Pickle(carrot)
            assert(carrotPickle == expectedCarrotPickle)
          }
          "unpickle"-{
            val unpickle = Unpickle[EdiblePlant].from(expectedCarrotPickle).get
            assert(carrot == unpickle)
          }
        }
        "null"-{
          val pickle: P = cyclicConfig.makeNull

          "pickle"-{
            assert(Pickle(null: EdiblePlant) == pickle)
          }
          "unpickle"-{
            val unpickle = Unpickle[EdiblePlant].from(pickle).get
            assert(null == unpickle)
          }
        }
      }
      "maps"-{
        val favoriteFoods = Map(ben -> apple, parent -> carrot)

        "pickle"-{
          val favoritePickles = Pickle(favoriteFoods)
          assert(favoritePickles == expectedMapPickle)}

        "unpickle"-{
          val unpickle = Unpickle[Map[Person, EdiblePlant]].from(expectedMapPickle).get
          assert(unpickle == favoriteFoods)}
      }
      "cycle00handling"-{
        assert(brothers._1.parent eq brothers._2.parent)

        val pickle = Pickle(brothers)
        val afterPickling = Unpickle[(PersonalDetails, PersonalDetails)].from(pickle).get

        val p1 = afterPickling._1.parent
        val p2 = afterPickling._2.parent
        assert(p1 eq p2)
      }
    }
    "with00acyclic00config"-{
      implicit val acyclicConfig: PConfig[P] = configFactory(false)

      "over00cyclic00structure"-{

        assert(brothers._1.parent eq brothers._2.parent)

        val pickleState = PickleState()
        val pickle = Pickle(brothers, pickleState)

        val unpickleState = mutable.Map.empty[String, Any]
        val afterPickling = Unpickle[(PersonalDetails, PersonalDetails)].from(pickle, unpickleState).get

        val p1 = afterPickling._1.parent
        val p2 = afterPickling._2.parent

        "shared00structures00are00duplicated"-{assert(!(p1 eq p2))}

        "data00is00preserved"-{
          assert(afterPickling._1 == benDetails)
          assert(afterPickling._2 == brotherDetails)
        }

        "no00id00tags00added00to00pickle"-{assert(pickle == stripIdTags(pickle))}

        "no00state00during00pickle"-{assert(pickleState.refs.isEmpty)}
        "no00state00during00unpickle"-{assert(unpickleState.isEmpty)}
      }
    }
  }
}