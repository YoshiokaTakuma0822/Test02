export type MessageType = 'CHAT' | 'JOIN' | 'LEAVE'

export interface User {
    id?: number
    name: string
    email?: string
}

export interface ChatMessage {
    id?: number
    sender: User
    content: string
    createdAt?: string
    type: MessageType
}
