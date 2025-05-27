import { ChatMessage, User } from "@/types/chat"
import { createContext, ReactNode, useContext } from "react"
import { useChat } from "./useChat"

interface ChatContextType {
    messages: ChatMessage[]
    activeUsers: User[]
    isConnecting: boolean
    sendMessage: (content: string) => void
    loadOlderMessages: (limit?: number) => Promise<void>
    currentUser: User | null // 現在のユーザー
}

export const ChatContext = createContext<ChatContextType | null>(null)

export function useChatContext() {
    const context = useContext(ChatContext)
    if (!context) {
        throw new Error("useChatContext must be used within a ChatProvider")
    }
    return context
}

interface ChatProviderProps {
    children: ReactNode
}

export function ChatProvider({ children }: ChatProviderProps) {
    const chat = useChat()

    return <ChatContext.Provider value={chat as ChatContextType}>{children}</ChatContext.Provider>
}
