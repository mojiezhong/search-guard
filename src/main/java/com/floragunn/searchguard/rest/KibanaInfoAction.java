/*
 * Copyright 2015-2017 floragunn GmbH
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
 * 
 */

package com.floragunn.searchguard.rest;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;

import com.floragunn.searchguard.privileges.PrivilegesEvaluator;
import com.floragunn.searchguard.support.ConfigConstants;
import com.floragunn.searchguard.user.User;

public class KibanaInfoAction extends BaseRestHandler {

    private final Logger log = LogManager.getLogger(this.getClass());
    private final PrivilegesEvaluator evaluator;
    private final ThreadContext threadContext;

    public KibanaInfoAction(final Settings settings, final RestController controller, final PrivilegesEvaluator evaluator, final ThreadPool threadPool) {
        super(settings);
        this.threadContext = threadPool.getThreadContext();
        this.evaluator = evaluator;
        controller.registerHandler(GET, "/_searchguard/kibanainfo", this);
        controller.registerHandler(POST, "/_searchguard/kibanainfo", this);       
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        return new RestChannelConsumer() {

            @Override
            public void accept(RestChannel channel) throws Exception {
                XContentBuilder builder = channel.newBuilder(); //NOSONAR
                BytesRestResponse response = null;
                
                try {
                    
                    final User user = (User)threadContext.getTransient(ConfigConstants.SG_USER);
                    final TransportAddress remoteAddress = (TransportAddress) threadContext.getTransient(ConfigConstants.SG_REMOTE_ADDRESS);

                    builder.startObject();
                    builder.field("user_name", user==null?null:user.getName());
                    builder.field("not_fail_on_forbidden_enabled", evaluator.notFailOnForbiddenEnabled());
                    builder.field("kibana_mt_enabled", evaluator.multitenancyEnabled());
                    builder.field("kibana_index", evaluator.kibanaIndex());
                    builder.field("kibana_server_user", evaluator.kibanaServerUsername());
                    //builder.field("kibana_index_readonly", evaluator.kibanaIndexReadonly(user, remoteAddress));
                    builder.endObject();

                    response = new BytesRestResponse(RestStatus.OK, builder);
                } catch (final Exception e1) {
                    log.error(e1.toString(),e1);
                    builder = channel.newBuilder(); //NOSONAR
                    builder.startObject();
                    builder.field("error", e1.toString());
                    builder.endObject();
                    response = new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder);
                } finally {
                    if(builder != null) {
                        builder.close();
                    }
                }

                channel.sendResponse(response);
            }
        };
    }

    @Override
    public String getName() {
        return "Kibana Info Action";
    }
    
    
}
