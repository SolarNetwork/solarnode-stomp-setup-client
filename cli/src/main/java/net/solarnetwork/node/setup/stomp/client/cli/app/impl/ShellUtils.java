/* ==================================================================
 * ShellUtils.java - 18/08/2021 4:09:49 PM
 * 
 * Copyright 2021 SolarNetwork Foundation
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.setup.stomp.client.cli.app.impl;

import java.util.Collection;

import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.util.StringUtils;

/**
 * Utilities for shell operations.
 * 
 * @author matt
 * @version 1.0
 */
public final class ShellUtils {

  private ShellUtils() {
    // don't construct me
  }

  /**
   * Render a datum list table.
   * 
   * @param list
   *          the list
   * @return a table
   */
  public static String renderDatumTable(Collection<GeneralDatum> list) {
    Object[][] data = new Object[list.size()][];
    int i = 0;
    for (GeneralDatum d : list) {
      // @formatter:off
      data[i] = new Object[] {
          d.getSourceId(),
          d.getTimestamp(),
          StringUtils.delimitedStringFromMap(d.getSampleData(), " = ", ",\n"), 
      };
      // @formatter:on
      i++;
    }

    TableModel model = new ArrayTableModel(data);
    TableBuilder tableBuilder = new TableBuilder(model);
    tableBuilder.addFullBorder(BorderStyle.fancy_light);

    return tableBuilder.build().render(100);
  }

}
