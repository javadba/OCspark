package org.openchai.db

import java.io.FileReader
import java.sql.{Connection, ResultSet, Statement}
import java.util.Properties

import scala.collection.mutable.ArrayBuffer

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
object Db {
  def connect(driver: String, url: String, user: String, pwd: String): Connection = {
    import java.sql._
    println(s"Connecting to user@$url ..")
    Class.forName(driver)
    val conn = DriverManager.getConnection(url, user, pwd)
    conn
  }
  def connect(pfile: String, group: String): Connection = {
    val p = new Properties()
      p.load(new FileReader(pfile))
    import collection.JavaConverters._
    val m = p.asScala.filterKeys(_.startsWith(group))
    connect(m(s"$group.driver"),m(s"$group.url"),m(s"$group.user"),m(s"$group.password"))
  }

  def exec(conn: Connection, sql: String) = {
    var stmt: Statement = null
    try {
      stmt = conn.createStatement
      val res = stmt.execute(sql)
      val rows = if (res) {
        stmt.getUpdateCount
      } else -1
      rows
    } finally {
      if (stmt != null) stmt.close
    }
  }

  def query(conn: Connection, query: String) = {
    var stmt: Statement = null
    var rs: ResultSet = null
    try {
      stmt = conn.createStatement
      rs = stmt.executeQuery(query)
      val rsm = rs.getMetaData
      val cnames = (1 to  rsm.getColumnCount).map(rsm.getColumnName)
      val ab = ArrayBuffer[Map[String,Any]]()
      while (rs.next) {
        ab += cnames.zip((1 to rsm.getColumnCount).map(rs.getObject)).toMap[String, Any]
      }
      ab
    } finally {

      if (rs != null) rs.close
      if (stmt != null) stmt.close
    }
  }

  def main(args: Array[String]): Unit = {
    val conn = connect("src/test/resources/h2.properties","h2")
    val create = """create table foo(id identity primary key, name varchar, metric int)"""
    val insert = """insert into foo(name, metric) values ($$name, $$metric)"""
    exec(conn,create)
    val nrows = 1000
    val rand = new java.util.Random
    for (r <- 0 until nrows) {
      exec(conn, insert.replace("$$name", s"row${rand.nextInt(100)}").replace("$$metric", "" + r * rand.nextInt(5)))
    }
    val res  = query(conn,""" select name, count(1) from (select rownum() as row, name, count(1) from foo group by name order by count(1) desc) where row < 5""")
    println(res)


  }

}
