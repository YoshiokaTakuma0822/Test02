import { ChatMessage, User } from "@/types/chat"

/**
 * Create a new user with random name and email
 */
export async function createUser(): Promise<User> {
    const response = await fetch("/api/users", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            name: `User${Math.floor(Math.random() * 1000)}`,
            email: `user${Math.floor(Math.random() * 1000)}@example.com`,
        }),
    })
    if (!response.ok) throw new Error(`Failed to create user: ${response.status}`)
    return response.json()
}

/**
 * Fetch recent chat messages
 */
export async function getRecentMessages(): Promise<ChatMessage[]> {
    const response = await fetch("/api/messages/recent")
    if (!response.ok) throw new Error(`Failed to fetch messages: ${response.status}`)
    return response.json()
}

/**
 * Fetch active users
 */
export async function getActiveUsers(): Promise<User[]> {
    const response = await fetch("/api/users/active")
    if (!response.ok) throw new Error(`Failed to fetch active users: ${response.status}`)
    return response.json()
}

/**
 * Send a new chat message
 */
export async function sendMessage(message: { sender: User; content: string; type: string }): Promise<void> {
    const response = await fetch("/api/messages", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(message),
    })
    if (!response.ok) throw new Error(`Failed to send message: ${response.status}`)
}

/**
 * Fetch older chat messages before a specific message ID
 */
export async function getMessagesBefore(
    beforeId: number,
    limit: number = 20
): Promise<ChatMessage[]> {
    const response = await fetch(`/api/messages?beforeId=${beforeId}&limit=${limit}`)
    if (!response.ok) throw new Error(`Failed to fetch older messages: ${response.status}`)
    return response.json()
}
