package org.openchai.registry

import org.scalatest.{FlatSpec, ShouldMatchers}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class DbSpec extends FlatSpec with ShouldMatchers {
  it should "do something" in
    DbSpec.main(Array.empty[String])
}
object DbSpec {

  def main(args: Array[String]): Unit = {
    import org.openchai.db.Db._
//    val conn = connect("registry/src/test/scala/resources/h2.properties","h2")
    val conn = connect(s"${System.getProperty("user.dir")}/src/test/scala/resources/h2.properties","h2")
    val create = """drop table if exists foo; create table foo(id identity primary key, name varchar, metric int)"""
    val insert = """insert into foo(name, metric) values ('$$name', $$metric)"""
    exec(conn,create)
    val nrows = 1000
    val rand = new java.util.Random
    for (r <- 0 until nrows) {
      exec(conn, insert.replace("$$name", s"row${rand.nextInt(100)}").replace("$$metric", "" + r * rand.nextInt(5)))
    }
    val res  = query(conn,
      """
        select a.name, cnt from
        (select name, count(1) as cnt
          from foo
          group by name
          order by count(1) desc
        ) a
        limit 5
        """.stripMargin)
    println(res)

  }

}
