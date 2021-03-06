/*
 * Copyright 2012 zhongl
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

package com.github.zhongl.api;

import com.google.common.util.concurrent.FutureCallback;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
class WriteOperation<V> implements FutureCallback<Void> {

    private final V attachment;
    private final FutureCallback<Void> origin;

    public WriteOperation(V attachment, FutureCallback<Void> origin) {
        this.attachment = attachment;
        this.origin = origin;
    }

    @Override
    public void onSuccess(Void result) {
        origin.onSuccess(result);
    }

    @Override
    public void onFailure(Throwable t) {
        origin.onFailure(t);
    }

    public V attachement() {
        return attachment;
    }

}
