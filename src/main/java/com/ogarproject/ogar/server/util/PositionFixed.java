/**
 * This file is part of Ogar.
 *
 * Ogar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ogar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ogar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ogarproject.ogar.server.util;

import com.ogarproject.ogar.server.config.OgarConfig;

/**
 * @author Calypso
 */

public class PositionFixed {
	public double x, y, angle;
    private OgarConfig configuration;
	public PositionFixed(double x, double y, double angle)
	{
		double r = 40;
        if ((x - r) < configuration.world.border.left) {
            angle = 6.28 - angle;
            x = configuration.world.border.left + r;
        }
        if ((x + r) > configuration.world.border.right) {
            angle = 6.28 - angle;
            x = configuration.world.border.right - r;
        }
        if ((y - r) < configuration.world.border.top) {
            angle = (angle <= 3.14) ? 3.14 - angle : 9.42 - angle;
            y = configuration.world.border.top + r;
        }
        if ((y + r) > configuration.world.border.bottom) {
            angle = (angle <= 3.14) ? 3.14 - angle : 9.42 - angle;
            y = configuration.world.border.bottom - r;
        }
        this.x = x; this.y = y; this.angle = angle;
	}


	public PositionFixed(double x, double y)
	{
		double r = 40;
        if ((x - r) < configuration.world.border.left) {
            x = configuration.world.border.left + r;
        }
        if ((x + r) > configuration.world.border.right) {
            x = configuration.world.border.right - r;
        }
        if ((y - r) <configuration.world.border.top) {
            y = configuration.world.border.top + r;
        }
        if ((y + r) > configuration.world.border.bottom) {
            y = configuration.world.border.bottom - r;
        }
        this.x = x; this.y = y; this.angle = 0;
	}
}