/*
 *  This file is part of GenericMultiplayerConnector.

    GenericMultiplayerConnector is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GenericMultiplayerConnector is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GenericMultiplayerConnector.  If not, see <http://www.gnu.org/licenses/>.

 */

package ssmith.util;

public class Interval {

	private long next_check_time, duration_ms;

	public Interval(long duration_ms) {
		this(duration_ms, false);
	}
	
	
	public Interval(long _duration_ms, boolean fire_now) {
		super();
		this.duration_ms = _duration_ms;
		if (fire_now) {
			this.next_check_time = System.currentTimeMillis(); // Fire straight away
		} else {
			this.next_check_time = System.currentTimeMillis() + duration_ms;
		}
	}
	
	
	public void restartTimer() {
		this.next_check_time = System.currentTimeMillis() + duration_ms;
	}

	
	public void setInterval(long amt, boolean restart) {
		duration_ms = amt;
		
		if (restart) {
			this.restartTimer();
		}
	}

	
	public boolean hitInterval() {
		if (System.currentTimeMillis() >= this.next_check_time) { // System.currentTimeMillis() - this.next_check_time
			this.restartTimer();
			return true;
		}
		return false;
	}
	
	
	public void fireInterval() {
		this.next_check_time = System.currentTimeMillis(); // Fire straight away
	}

}

