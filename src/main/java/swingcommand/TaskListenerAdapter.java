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
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * An adapter, in the sense of Swing listener adapters, which can be exteded by classes
 * which want to inherit a default implementation of the observer methods, overriding
 * only those which they are interested in
 */
public class TaskListenerAdapter<P> implements TaskListener<P> {

    public void pending(Task task) {}

    public void started(Task task) {}

    public void progress(Task task, P progress) {}

    public void success(Task task) {}

    public void error(Task task, Throwable error) {}

    public void finished(Task task) {}
}