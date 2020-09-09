// IChatService.aidl
package io.locha.p2p.service;

/**
 * Interface for communicating with ChatService class
 */
interface IChatService {
    /**
     * Dial a peer using it's address
     */
    void dial(String multiaddr);

    /**
     * Send a message
     */
    void sendMessage(String contents);

    /**
     * Get our PeerId
     * @return The PeerId as an String.
     */
    String getPeerId();
}