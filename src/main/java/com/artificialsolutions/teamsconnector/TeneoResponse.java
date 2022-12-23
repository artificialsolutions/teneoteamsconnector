package com.artificialsolutions.teamsconnector;

import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

/**
 * Specifies a Teneo engine response with its source data. 
 */
public class TeneoResponse {

    /**
     * The JSON source received from Teneo engine.
     */
    private final JsonNode json;

    /**
     * The status of a Teneo engine response indicating if it is an error etc.
     */
    private final TeneoResponseStatus status = new TeneoResponseStatus();

    /**
     * The content of a Teneo engine response (the text, adaptive cards etc).
     */
    private TeneoResponseOutput output;

    /**
     * Constructs an instance of this class.
     *
     * @param json the JSON object to construct this object from. 
     */
    public TeneoResponse(JsonNode json) {
        this.json = json;
    }

    /**
     * Gets the JSON source received from Teneo engine.
     *
     * @return The JSON source received from Teneo engine.
     */
    public JsonNode getJson() {
        return json;
    }

    /**
     * Gets the status of a Teneo engine response indicating if it is an error etc.
     *
     * @return The status of a Teneo engine response indicating if it is an error etc.
     */
    public TeneoResponseStatus getStatus() {
        return status;
    }

    /**
     * Gets the content of a Teneo engine response (the text, adaptive cards etc).
     *
     * @return The content of a Teneo engine response (the text, adaptive cards etc).
     */
    public TeneoResponseOutput getOutput() {
        return output;
    }

    public static class Parser {

        public static TeneoResponse parse(final JsonNode json) {
            var response = new TeneoResponse(json);

            parseNode(response, json, "status", JsonNodeType.NUMBER, Parser::parseStatus);
            return response;
        }

        private static void parseStatus(final TeneoResponse response, final JsonNode statusNode) {
            switch (statusNode.asInt()) {
                case 0:
                    // OK - Teneo engine answered
                    parseNode(response, response.json, "output", JsonNodeType.OBJECT, Parser::parseOutput);
                    break;
                case -1:
                    // Error - Teneo engine returned an error
                    parseNode(response, response.json, "message", JsonNodeType.STRING,
                            (r, n) -> r.status.setErrorMessageFromTeneo(n.asText()));
                    break;
                default:
                    // Error - Teneo engine returned a status code unexpected in this context
                    response.status.setErrorMessage(String.format(
                            "Teneo response has an unexpected value of the [status] property for a regular request: %s",
                            statusNode.asInt()));
            }
        }

        private static void parseOutput(final TeneoResponse response, final JsonNode outputNode) {
            response.output = new TeneoResponseOutput();

            parseNode(response, outputNode, "text", JsonNodeType.STRING, (r, n) -> r.output.setText(n.asText()));
            parseNodeIfExists(response, outputNode, "parameters", JsonNodeType.OBJECT, Parser::parseParameters);
        }

        private static void parseParameters(final TeneoResponse response, final JsonNode json) {
            if (json != null) {
                parseNodeIfExists(response, json, "msbotframework", JsonNodeType.STRING, (r, n) -> {
                    if (n != null) {
                        r.output.setAdaptiveCardContents(n.asText());
                    }
                });
                parseNodeIfExists(response, json, "outputTextSegmentIndexes", JsonNodeType.STRING,
                        (r, n) -> {
                            if (n != null) {
                                r.output.setTextSegmentIndexes(n.asText());
                            }
                        });
            }
        }

        private static void parseNode(final TeneoResponse response, final JsonNode parent, String fieldName,
                JsonNodeType jsonNodeType, BiConsumer<TeneoResponse, JsonNode> success) {
            String msg = null;
            var json = parent.get(fieldName);
            if (json == null) {
                msg = String.format("Teneo response has no [%s] property", fieldName);
            } else if (json.getNodeType() != jsonNodeType) {
                msg = String.format("Teneo response has [%s] of type %s, should be %s", fieldName, json.getNodeType(),
                        jsonNodeType);
            }

            if (msg != null) {
                response.status.setErrorMessage(msg);
            } else {
                success.accept(response, json);
            }
        }

        private static void parseNodeIfExists(final TeneoResponse response, final JsonNode parent, String fieldName,
                JsonNodeType jsonNodeType, BiConsumer<TeneoResponse, JsonNode> success) {
            String msg = null;
            var json = parent.get(fieldName);
            if (json != null && json.getNodeType() != jsonNodeType) {
                msg = String.format("Teneo response has [%s] of type %s, should be %s", fieldName, json.getNodeType(),
                        jsonNodeType);
            }

            if (msg != null) {
                response.status.setErrorMessage(msg);
            } else {
                success.accept(response, json);
            }
        }

        private Parser() {
        }
    }
}