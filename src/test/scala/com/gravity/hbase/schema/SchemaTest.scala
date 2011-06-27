package com.gravity.hbase.schema

import org.apache.hadoop.hbase.HBaseConfiguration
import org.junit.Test
import com.gravity.hbase.schema._

/*             )\._.,--....,'``.
 .b--.        /;   _.. \   _\  (`._ ,.
`=,-,-'~~~   `----(,_..'--(,_..'`-.;.'  */

object ExampleSchema extends Schema {
  implicit val conf = HBaseConfiguration.create

  class ExampleTable extends HbaseTable[ExampleTable](tableName="schema_example") {
    val meta = family[String,String,Any]("meta")
    val title = column(meta,"title", classOf[String])
    val url = column(meta,"url", classOf[String])
    val views = column(meta,"views", classOf[Long])

    val viewCounts = family[String,String,Long]("views")

    val viewCountsByDay = family[String,YearDay,Long]("viewsByDay")
  }

  val ExampleTable = new ExampleTable

}

class SchemaTest  {

  @Test def createAndDelete() {
    val create = ExampleSchema.ExampleTable.createScript()
    println(create)
  }

  def dumpViewMap(key:Long) {
    val dayViewsRes = ExampleSchema.ExampleTable.query.withKey(key).withColumnFamily(_.viewCountsByDay).single()
    val dayViewsMap = dayViewsRes.family(_.viewCountsByDay)

    for((yearDay, views) <- dayViewsMap) {
      println("Got yearday " + yearDay + " with views " + views)
    }
  }

  @Test def testPut() {
    ExampleSchema.ExampleTable
      .put("Chris").value(_.title,"My Life, My Times")
      .put("Joe").value(_.title,"Joe's Life and Times")
      .increment("Chris").value(_.views,10l)
      .execute()
    
    ExampleSchema.ExampleTable.put(1346l).value(_.title,"My kittens").execute

    ExampleSchema.ExampleTable.put(1346l).valueMap(_.viewCounts, Map("Today" -> 61l, "Yesterday" -> 86l)).execute

    val dayMap = Map(
      YearDay(2011,63) -> 64l,
      YearDay(2011,64) -> 66l,
      YearDay(2011,65) -> 67l
    )

    val id = 1346l

    ExampleSchema.ExampleTable.put(id).valueMap(_.viewCountsByDay, dayMap).execute

    println("Dumping after map insert")
    dumpViewMap(id)

    ExampleSchema.ExampleTable.increment(id).valueMap(_.viewCountsByDay, dayMap).execute

    println("Dumping after increment")
    dumpViewMap(id)

    ExampleSchema.ExampleTable.delete(id).family(_.viewCountsByDay).execute
    println("Dumping after delete")
    dumpViewMap(id)

    val views = ExampleSchema.ExampleTable.query.withKey("Chris").withColumn(_.views).single().column(_.views)

    println("Views: " + views.get)
  }
}