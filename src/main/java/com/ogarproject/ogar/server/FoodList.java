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
package com.ogarproject.ogar.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.ogarproject.ogar.server.entity.impl.FoodImpl;

/**
 * @author Calypso
 */

public class FoodList
{
    private final Set<FoodImpl> foods = new HashSet<>();

    public Collection<FoodImpl> getAllFood() {
        return foods;
    }

    public void addFood(FoodImpl food) {
    	foods.add(food);
    }

    public void removeFood(FoodImpl food) {
    	foods.remove(food);
    }
}