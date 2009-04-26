/*
 * Copyright 2009 Object Definitions Ltd.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 14-Sep-2008
 * Time: 18:07:43
 * To change this template use File | Settings | File Templates.
 */
public enum ExecutionState {

    NOT_RUN,
    PENDING,
    STARTED,
    SUCCESS,
    ERROR;

    public boolean isFinalState() {
        return this == SUCCESS || this == ERROR;
    }
}