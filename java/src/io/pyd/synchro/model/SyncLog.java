/*
 * Copyright 2012 Charles du Jeu <charles (at) pyd.io>
 * This file is part of Pydio.
 *
 * Pydio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Pydio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pydio.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The latest code can be found at <http://pyd.io/>..
 *
 */
package io.pyd.synchro.model;

import info.ajaxplorer.client.model.Node;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class SyncLog {

	public static String LOG_STATUS_SUCCESS = "success";
	public static String LOG_STATUS_INTERRUPT = "interrupt";
	public static String LOG_STATUS_ERRORS = "errors";
	public static String LOG_STATUS_CONFLICTS = "conflicts";
	
	@DatabaseField(generatedId=true)
	private int id;
	@DatabaseField
	public long jobDate;
	@DatabaseField
	public String jobStatus;
	@DatabaseField
	public String jobSummary;
	@DatabaseField(foreign=true,index=true)
	public Node synchroNode;
	
}
