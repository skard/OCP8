package com.google.cloud.examples.pubsub.snippets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.Identity;
import com.google.cloud.Role;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient.ListTopicSubscriptionsPagedResponse;
import com.google.cloud.pubsub.v1.TopicAdminClient.ListTopicsPagedResponse;
import com.google.common.collect.Iterables;
import com.google.iam.v1.Policy;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class ITTopicAdminClientSnippets {

    private static final String NAME_SUFFIX = UUID.randomUUID().toString();

    private static String projectId;

    private static TopicAdminClientSnippets topicAdminClientSnippets;

    private static String[] topics = {
            formatForTest("topic-1"), formatForTest("topic-2"),
    };
    private static String[] subscriptions = {
            formatForTest("subscription-1"), formatForTest("subscription-2")
    };

    @Rule
    public Timeout globalTimeout = Timeout.seconds(300);

    private static String formatForTest(String resourceName) {
        return resourceName + "-" + NAME_SUFFIX;
    }

    @BeforeClass
    public static void beforeClass() {
        topicAdminClientSnippets = new TopicAdminClientSnippets();
        projectId = topicAdminClientSnippets.getProjectId();
    }

    @Before
    public void setUp() throws Exception {
        Cleanup.deleteTestTopicsAndSubscriptions(projectId, topics, subscriptions);
    }

    @Test
    public void topicAddedIsSameAsRetrieved() throws Exception {
        String topicName = topics[0];
        Topic topicAdded = topicAdminClientSnippets.createTopic(topicName);
        assertNotNull(topicAdded);
        Topic topicRetrieved = topicAdminClientSnippets.getTopic(topicName);
        assertEquals(topicAdded, topicRetrieved);
    }

    @Test
    public void listTopicsRetreivesAddedTopics() throws Exception {
        List<Topic> addedTopics = new ArrayList<>();
        String topicName1 = topics[0];
        addedTopics.add(topicAdminClientSnippets.createTopic(topicName1));
        String topicName2 = topics[1];
        addedTopics.add(topicAdminClientSnippets.createTopic(topicName2));

        boolean[] topicFound = {false, false};
        ListTopicsPagedResponse response = topicAdminClientSnippets.listTopics();

        assertNotNull(response);
        Iterable<Topic> topics = response.iterateAll();
        for (int i = 0; i < 2; i++) {
            if (!topicFound[i]) {
                topicFound[i] = Iterables.contains(topics, addedTopics.get(i));
            }
        }

        assertTrue(topicFound[0] && topicFound[1]);
    }

    @Test
    public void listTopicSubscriptionsRetrievesAddedSubscriptions() throws Exception {
        List<String> addedSubscriptions = new ArrayList<>();
        String topicName1 = topics[0];
        topicAdminClientSnippets.createTopic(topicName1);
        String subscriptionName1 = subscriptions[0];
        String subscriptionName2 = subscriptions[1];
        addedSubscriptions.add(createSubscription(topicName1, subscriptionName1));
        addedSubscriptions.add(createSubscription(topicName1, subscriptionName2));

        boolean[] subFound = {false, false};

        ListTopicSubscriptionsPagedResponse response =
                topicAdminClientSnippets.listTopicSubscriptions(topicName1);

        assertNotNull(response);
        Iterable<String> subscriptions = response.iterateAll();
        for (int i = 0; i < 2; i++) {
            if (!subFound[i]) {
                subFound[i] = Iterables.contains(subscriptions, addedSubscriptions.get(i));
            }
        }
        assertTrue(subFound[0] && subFound[1]);
    }

    @Test(expected = ApiException.class)
    public void deletedTopicIsNotRetrievableAndThrowsException() throws Exception {
        String topicName = topics[0];
        Topic topicAdded = topicAdminClientSnippets.createTopic(topicName);
        assertNotNull(topicAdded);
        ProjectTopicName formattedName = topicAdminClientSnippets.deleteTopic(topicName);
        assertNotNull(formattedName);
        topicAdminClientSnippets.getTopic(topicName);
    }

    @Test
    public void topicPolicyIsCorrectlyRetrieved() throws Exception {
        String topicName = topics[0];
        topicAdminClientSnippets.createTopic(topicName);
        Policy policy = topicAdminClientSnippets.getTopicPolicy(topicName);
        assertNotNull(policy);
    }

    @Test
    public void replaceTopicPolicyAndTestPermissionsIsSuccessful() throws Exception {
        String topicName = topics[0];
        topicAdminClientSnippets.createTopic(topicName);
        Policy policy = topicAdminClientSnippets.replaceTopicPolicy(topicName);
        assertNotNull(policy.getBindingsCount());
        assertTrue(policy.getBindings(0).getRole().equalsIgnoreCase(Role.viewer().toString()));
        assertTrue(
                policy
                        .getBindings(0)
                        .getMembers(0)
                        .equalsIgnoreCase(Identity.allAuthenticatedUsers().toString()));
        TestIamPermissionsResponse response = topicAdminClientSnippets.testTopicPermissions(topicName);
        assertTrue(response.getPermissionsList().contains("pubsub.topics.get"));
    }

    private String createSubscription(String topic, String subscriptionName) throws Exception {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            Subscription subscription =
                    subscriptionAdminClient.createSubscription(
                            ProjectSubscriptionName.of(projectId, subscriptionName),
                            ProjectTopicName.of(projectId, topic),
                            PushConfig.getDefaultInstance(),
                            0);
            return subscription.getName();
        }
    }

    @After
    public void tearDown() throws Exception {
        Cleanup.deleteTestTopicsAndSubscriptions(projectId, topics, subscriptions);
    }
}
