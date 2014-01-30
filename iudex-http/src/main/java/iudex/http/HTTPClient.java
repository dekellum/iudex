/*
 * Copyright (c) 2008-2014 David Kellum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iudex.http;

/**
 * HTTP Client abstraction. This interface is used to hide HTTP client
 * implementation details and enable pluggability. In particular synchronous
 * (executes in this thread) and asynchronous (call handler in the future) modes
 * are abstracted.
 */
public interface HTTPClient
{
    /**
     * Create a new generic session, suitable for adding request details and
     * calling request().
     */
    public HTTPSession createSession();

    /**
     * Perform request as defined by session and relay results to handler. This
     * call may return immediately or block until initial request processing is
     * complete. In any case, the handler should call session.close() when the
     * session is no longer needed.
     */
    public void request( HTTPSession session, ResponseHandler handler );
}
