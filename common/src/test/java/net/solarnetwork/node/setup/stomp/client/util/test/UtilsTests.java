/* ==================================================================
 * UtilsTests.java - 3/08/2020 11:31:53 AM
 * 
 * Copyright 2020 SolarNetwork Foundation
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

package net.solarnetwork.node.setup.stomp.client.util.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.solarnetwork.node.setup.stomp.client.util.Utils;

/**
 * Test cases for the {@link Utils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UtilsTests {

  @Test
  public void getResource_ok() {
    String result = Utils.getResource("hello-world.txt", UtilsTests.class);
    assertThat("Resource loaded", result, equalTo("Hello, world."));
  }

  @Test
  public void getResource_404() {
    Assertions.assertThrows(RuntimeException.class, () -> {
      Utils.getResource("non-existent.txt", UtilsTests.class);
    }, "Requesting a non-existing resource throws exception.");

  }

}
