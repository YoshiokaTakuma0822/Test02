// lib/useChat.ts
import { sendMessage as apiSendMessage, createUser, getActiveUsers, getMessagesBefore, getRecentMessages } from "@/lib/api"
import { ChatMessage, User } from "@/types/chat"
import { Client } from '@stomp/stompjs'
import { useCallback, useEffect, useRef, useState } from "react"

export function useChat() {
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [isConnecting, setIsConnecting] = useState(true)
    const [activeUsers, setActiveUsers] = useState<User[]>([])
    const currentUserRef = useRef<User | null>(null)
    const clientRef = useRef<Client | null>(null)
    const lastActiveTimeRef = useRef<number>(Date.now())

    const updateMessages = useCallback(async () => {
        try {
            const recentMessages = await getRecentMessages()
            setMessages(prevMessages => {
                // Filter out duplicates and merge, then sort by id
                const newOnes = recentMessages.filter(msg => !prevMessages.some(pm => pm.id === msg.id))
                const merged = [...prevMessages, ...newOnes]
                return merged.sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
            })
        } catch (error) {
            console.error("Failed to fetch messages:", error)
        }
    }, [])

    const updateUsers = useCallback(async () => {
        try {
            const users = await getActiveUsers()
            setActiveUsers(users)
        } catch (error) {
            console.error("Failed to fetch users:", error)
        }
    }, [])

    const fetchInitialData = useCallback(async () => {
        try {
            await Promise.all([
                updateMessages(),
                updateUsers()
            ])
        } catch (error) {
            console.error("Failed to fetch initial data:", error)
        }
    }, [updateMessages, updateUsers])

    // WebSocket接続とデータ更新の設定
    useEffect(() => {
        let isMounted = true

        // バックグラウンドから復帰した時の再接続処理
        const handleVisibilityChange = async () => {
            if (document.visibilityState === 'visible') {
                const now = Date.now()
                const timeSinceLastActive = now - lastActiveTimeRef.current
                console.log(`App became visible after ${timeSinceLastActive}ms`)

                // 5秒以上バックグラウンドにいた場合は再接続
                if (timeSinceLastActive > 5000 && currentUserRef.current) {
                    console.log("App was in background for a while, checking connection...")

                    // WebSocketの状態をチェック
                    const needsReconnection = !clientRef.current ||
                        !clientRef.current.connected ||
                        (clientRef.current.webSocket && clientRef.current.webSocket.readyState !== WebSocket.OPEN)

                    if (needsReconnection) {
                        try {
                            console.log("Reconnecting after returning from background...")
                            setIsConnecting(true)

                            // 古い接続があれば切断
                            if (clientRef.current) {
                                await clientRef.current.deactivate()
                            }

                            // 初期データの取得
                            await fetchInitialData()
                            await setupWebSocket(currentUserRef.current)
                        } catch (error) {
                            console.error("Failed to reconnect after background:", error)
                            setIsConnecting(false)
                        }
                    } else {
                        // 接続は生きているが、データを更新
                        console.log("Connection is alive, refreshing data...")
                        await fetchInitialData()
                    }
                }
                lastActiveTimeRef.current = now
            } else if (document.visibilityState === 'hidden') {
                lastActiveTimeRef.current = Date.now()
                console.log("App went to background")
            }
        }

        const setupWebSocket = async (user: User) => {
            try {
                const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
                const brokerURL = process.env.NODE_ENV === 'development'
                    ? `${protocol}://${window.location.hostname}:8080/ws`
                    : `${protocol}://${window.location.host}/ws`

                const client = new Client({
                    brokerURL,
                    connectHeaders: {
                        userId: user.id?.toString() ?? ""  // nullチェックを追加
                    },
                    onConnect: async () => {
                        try {
                            // 初期データの取得
                            await fetchInitialData()
                            // メッセージ更新の購読
                            client.subscribe("/topic/chat.messages.update", async () => {
                                console.log("Received message update notification")
                                await updateMessages()
                            })
                            // ユーザー更新の購読
                            client.subscribe("/topic/chat.users.update", async () => {
                                console.log("Received users update notification")
                                await updateUsers()
                            })
                            setIsConnecting(false)
                        } catch (error) {
                            console.error("Failed to fetch initial data:", error)
                            setIsConnecting(false)
                        }
                    },
                    onDisconnect: async () => {
                        console.log("WebSocket disconnected")
                        setIsConnecting(true)
                        // 再接続時にユーザーリストを更新
                        await updateUsers()
                    },
                    onStompError: async (frame) => {
                        console.error("Stomp error:", frame)
                        setIsConnecting(true)
                        // エラー時にもユーザーリストを更新
                        await updateUsers()
                    }
                })

                client.activate()
                clientRef.current = client
            } catch (error) {
                console.error("Failed to setup WebSocket:", error)
                setIsConnecting(true)
            }
        }

        // 初期化処理を直接実行
        (async () => {
            try {
                const user = await createUser()
                currentUserRef.current = user
                if (isMounted) {
                    // 初期データの取得
                    await fetchInitialData()
                    await setupWebSocket(user)
                    document.addEventListener('visibilitychange', handleVisibilityChange)
                }
            } catch (error) {
                console.error("Failed to setup chat:", error)
                setIsConnecting(false)
            }
        })()

        return () => {
            isMounted = false
            if (clientRef.current) {
                // deactivateはPromiseを返す可能性があるが、
                // useEffectのクリーンアップ関数ではasync/awaitは使えないため、
                // Promiseをキャッチして静かに処理する
                Promise.resolve(clientRef.current.deactivate()).catch(error => {
                    console.error("Failed to deactivate WebSocket:", error)
                })
            }
            // イベントリスナーのクリーンアップ
            document.removeEventListener('visibilitychange', handleVisibilityChange)
        }
    }, [fetchInitialData, updateMessages, updateUsers])

    const sendMessage = async (content: string) => {
        const user = currentUserRef.current
        if (user) {
            try {
                await apiSendMessage({
                    sender: user,
                    content,
                    type: "CHAT"
                })
                await updateMessages() // メッセージを送信後に最新のメッセージを取得
            } catch (error) {
                console.error("Failed to send message:", error)
            }
        } else {
            console.warn("Cannot send message: No user")
        }
    }

    // Load older messages and prepend to existing list
    const loadOlderMessages = async (limit: number = 20): Promise<void> => {
        try {
            if (messages.length === 0) return
            const oldestId = messages[0].id
            if (oldestId === undefined) return
            const olderMessages = await getMessagesBefore(oldestId, limit)
            if (olderMessages.length === 0) return
            setMessages(prevMessages => {
                // Filter out duplicates and merge, then sort by id
                const newOnes = olderMessages.filter(msg => !prevMessages.some(pm => pm.id === msg.id))
                const merged = [...prevMessages, ...newOnes]
                return merged.sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
            })
        } catch (error) {
            console.error("Failed to load older messages:", error)
        }
    }

    return { messages, isConnecting, sendMessage, activeUsers, loadOlderMessages, currentUser: currentUserRef.current }
}
