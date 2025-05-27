"use client"

import { ActiveUsersList, ChatInput, MessageList, UserAvatar } from "@/components/chat"
import { ChatProvider, useChatContext } from "@/lib/ChatContext"

function ChatContent() {
    const { isConnecting } = useChatContext()

    if (isConnecting) {
        return (
            <div className="flex items-center justify-center flex-1 text-gray-500">
                接続中...
            </div>
        )
    }

    return (
        <>
            <ActiveUsersList />
            <MessageList />
            <ChatInput />
        </>
    )
}

export default function ChatPage() {
    return (
        <ChatProvider>
            <ChatPageContent />
        </ChatProvider>
    )
}

function ChatPageContent() {
    const { currentUser } = useChatContext()

    return (
        <main className="flex flex-col h-screen">
            <div className="p-4 border-b flex items-center justify-between">
                <h1 className="text-2xl font-bold">チャットルーム</h1>
                <UserAvatar user={currentUser} />
            </div>
            <div className="flex-1 flex flex-col min-h-0">
                <ChatContent />
            </div>
        </main>
    )
}
