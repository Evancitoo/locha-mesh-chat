/*
 * Copyright 2020 Locha Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.locha.p2p.runtime;

/**
 * Events of the Chat service.
 */
public interface ChatServiceEvents {
    /**
     * @param contents Message contents
     */
    public void onNewMessage(String contents);

    /**
     * One of the listeners has reported a new local listening address
     *
     * @param multiaddr Listening address in Multiaddr format.
     */
    public void onNewListenAddr(String multiaddr);
}