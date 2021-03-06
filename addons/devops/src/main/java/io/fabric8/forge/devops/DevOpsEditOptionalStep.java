/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.devops;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;

import io.fabric8.devops.ProjectConfig;
import io.fabric8.forge.addon.utils.CommandHelpers;
import io.fabric8.forge.addon.utils.StopWatch;
import io.fabric8.letschat.LetsChatClient;
import io.fabric8.letschat.RoomDTO;
import io.fabric8.taiga.ProjectDTO;
import io.fabric8.taiga.TaigaClient;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevOpsEditOptionalStep extends AbstractDevOpsCommand {
    private static final transient Logger LOG = LoggerFactory.getLogger(DevOpsEditOptionalStep.class);

    @Inject
    @WithAttributes(label = "Chat room", description = "Name of chat room to use for this project")
    private UIInput<String> chatRoom;

    @Inject
    @WithAttributes(label = "IssueTracker Project name", description = "Name of the issue tracker project")
    private UIInput<String> issueProjectName;

    @Inject
    @WithAttributes(label = "Code review", description = "Enable code review of all commits")
    private UIInput<Boolean> codeReview;

    private LetsChatClient letsChat;
    private TaigaClient taigaClient;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass())
                .category(Categories.create(AbstractDevOpsCommand.CATEGORY))
                .name(AbstractDevOpsCommand.CATEGORY + ": Configure Optional")
                .description("Configure the Project options for the new project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        StopWatch watch = new StopWatch();

        final UIContext context = builder.getUIContext();

        letsChat = (LetsChatClient) builder.getUIContext().getAttributeMap().get("letsChatClient");
        taigaClient = (TaigaClient) builder.getUIContext().getAttributeMap().get("taigaClient");

/*        chatRoom.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                // TODO: call only once to init getChatRoomNames
                return filterCompletions(getChatRoomNames(), value);
            }
        });
        issueProjectName.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                // TODO: call only once to init getIssueProjectNames
                return filterCompletions(getIssueProjectNames(), value);
            }
        });*/

        // lets initialise the data from the current config if it exists
        ProjectConfig config = (ProjectConfig) context.getAttributeMap().get("projectConfig");
        if (config != null) {
            CommandHelpers.setInitialComponentValue(chatRoom, config.getChatRoom());
            CommandHelpers.setInitialComponentValue(issueProjectName, config.getIssueProjectName());
            CommandHelpers.setInitialComponentValue(codeReview, config.getCodeReview());
        }

        builder.add(chatRoom);
        builder.add(issueProjectName);
        builder.add(codeReview);

        LOG.info("initializeUI took " + watch.taken());
    }

    public static Iterable<String> filterCompletions(Iterable<String> values, String inputValue) {
        boolean ignoreFilteringAsItBreaksHawtio = true;
        if (ignoreFilteringAsItBreaksHawtio) {
            return values;
        } else {
            List<String> answer = new ArrayList<>();
            String lowerInputValue = inputValue.toLowerCase();
            for (String value : values) {
                if (value != null) {
                    if (value.toLowerCase().contains(lowerInputValue)) {
                        answer.add(value);
                    }
                }
            }
            return answer;
        }
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        context.getUIContext().getAttributeMap().put("chatRoom", chatRoom.getValue());
        context.getUIContext().getAttributeMap().put("issueProjectName", issueProjectName.getValue());
        context.getUIContext().getAttributeMap().put("codeReview", codeReview.getValue());

        return null;
    }

    private Iterable<String> getIssueProjectNames() {
        Set<String> answer = new TreeSet<>();
        try {
            if (taigaClient != null) {
                List<ProjectDTO> projects = null;
                try {
                    projects = taigaClient.getProjects();
                } catch (Exception e) {
                    LOG.warn("Failed to load chat projects! " + e, e);
                }
                if (projects != null) {
                    for (ProjectDTO project : projects) {
                        String name = project.getName();
                        if (name != null) {
                            answer.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to get issue project names: " + e, e);
        }
        return answer;
    }

    private Iterable<String> getChatRoomNames() {
        Set<String> answer = new TreeSet<>();
        try {
            if (letsChat != null) {
                List<RoomDTO> rooms = null;
                try {
                    rooms = letsChat.getRooms();
                } catch (Exception e) {
                    LOG.warn("Failed to load chat rooms! " + e, e);
                }
                if (rooms != null) {
                    for (RoomDTO room : rooms) {
                        String name = room.getSlug();
                        if (name != null) {
                            answer.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to find chat room names: " + e, e);
        }
        return answer;
    }

}
