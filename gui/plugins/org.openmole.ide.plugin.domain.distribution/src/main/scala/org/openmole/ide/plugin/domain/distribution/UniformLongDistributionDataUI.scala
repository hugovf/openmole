/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.domain.distribution

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.domain.distribution._
import org.openmole.core.model.domain.Domain

class UniformLongDistributionDataUI extends UniformDistributionDataUI[Long] {

  val max = None

  val availableTypes = List("Long")

  def buildPanelUI(p: IPrototypeDataProxyUI) = new UniformDistributionPanelUI(this, p)

  def coreClass = classOf[UniformLongDistribution]

  def coreObject(prototype: IPrototypeDataProxyUI): Domain[Long] = new UniformLongDistribution
}
