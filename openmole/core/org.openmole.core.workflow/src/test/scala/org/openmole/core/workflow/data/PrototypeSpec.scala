/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.data

import org.openmole.core.context.Val
import org.scalatest._
import org.scalatest.junit._

import scala.collection.mutable.ListBuffer
import org.openmole.core.workflow.dsl._

class PrototypeSpec extends FlatSpec with Matchers {
  "ToArray of dim 0" should "return the prototype itself" in {
    val a = Val[Int]("a")
    a.toArray(0) should equal(a)
  }
}
