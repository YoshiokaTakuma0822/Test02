'use client'

import { Card } from "@/components/ui/card"
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from "@/components/ui/tooltip"
import { useChatContext } from "@/lib/ChatContext"
import { ChatMessage } from "@/types/chat"
import {
    differenceInMinutes,
    format,
    formatDistanceToNow,
} from "date-fns"
import { ja } from "date-fns/locale"
import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { VariableSizeList as List, VariableSizeList } from 'react-window'

// メッセージアイテムのレンダー関数
const MessageItem = ({ data, index, style }: {
    data: {
        messages: ChatMessage[]
        highlightedIds: number[]
        fadeInTime: number
        fadeOutTime: number
        loadTriggerCount: number
        topRef: React.RefObject<HTMLDivElement | null>
        observerRef: React.RefObject<IntersectionObserver | null>
        onIntersect: (el: HTMLElement) => void
    }
    index: number
    style: React.CSSProperties
}) => {
    const message = data.messages[index]
    const isTrigger = index === data.loadTriggerCount
    const isHighlighted = message.id != null && data.highlightedIds.includes(message.id)

    const renderMessage = (message: ChatMessage) => {
        switch (message.type) {
            case "JOIN":
                return `${message.sender.name}さんが参加しました`
            case "LEAVE":
                return `${message.sender.name}さんが退出しました`
            default:
                return message.content
        }
    }

    return (
        <div style={style}>
            <Card
                key={message.id != null ? message.id.toString() : index}
                data-id={message.id}
                ref={el => {
                    if (isTrigger && el) {
                        data.topRef.current = el
                        // 一度だけIntersectionObserverを設定
                        const observer = new IntersectionObserver((entries) => {
                            entries.forEach(entry => {
                                if (entry.isIntersecting) {
                                    data.onIntersect(el)
                                    observer.disconnect()
                                }
                            })
                        }, { threshold: 0.1 })
                        observer.observe(el)
                    }
                    if (isHighlighted && el && data.observerRef.current) {
                        data.observerRef.current.observe(el)
                    }
                }}
                style={{
                    transitionDuration: `${isHighlighted ? data.fadeInTime : data.fadeOutTime}ms`,
                    margin: '0 1rem 1rem 1rem'
                }}
                className={`p-4 transition-colors ease-in-out ${message.type !== 'CHAT'
                    ? 'bg-gray-50'
                    : isHighlighted
                        ? 'bg-blue-100'
                        : 'bg-white'
                    }`}
            >
                <div className="flex justify-between items-start">
                    <div>
                        <p className="font-semibold">
                            {message.sender.name}
                        </p>
                        <p className="mt-1">{renderMessage(message)}</p>
                    </div>
                    {(() => {
                        const date = new Date(
                            message.createdAt || Date.now()
                        )
                        const label =
                            differenceInMinutes(new Date(), date) < 1
                                ? "now"
                                : formatDistanceToNow(date, {
                                    addSuffix: true,
                                    locale: ja,
                                })
                        return (
                            <TooltipProvider>
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <time className="text-sm text-gray-500">
                                            {label}
                                        </time>
                                    </TooltipTrigger>
                                    <TooltipContent>
                                        {format(date, "yyyy/MM/dd HH:mm:ss", {
                                            locale: ja,
                                        })}
                                    </TooltipContent>
                                </Tooltip>
                            </TooltipProvider>
                        )
                    })()}
                </div>
            </Card>
        </div>
    )
}

export function MessageList({ loadTriggerCount = 1 }: { loadTriggerCount?: number }) {
    const { messages, loadOlderMessages, currentUser } = useChatContext()

    const isLoadingRef = useRef(false)
    // ref is typed as VariableSizeList
    const listRef = useRef<VariableSizeList>(null)
    const topRef = useRef<HTMLDivElement>(null)
    // pending adjust info - 履歴読み込み時のスクロール調整用
    const pendingAdjustRef = useRef<{
        targetId: string,
        initialCount: number,
        lastMessageId: string | null
    } | null>(null)

    // State to track if user is at bottom and if new messages arrived when scrolled up
    const [isAtBottom, setIsAtBottom] = useState(true)
    const [hasNewMessages, setHasNewMessages] = useState(false)
    // 初回参加時の基準時刻を保持（この時刻より新しいメッセージをハイライト対象とする）
    const [joinTimestamp, setJoinTimestamp] = useState<number | null>(null)
    // フェードイン・アウト時間設定（ms）
    const fadeInTime = 500
    const fadeOutTime = 1000
    // IntersectionObserverでカードの視界入りを検知
    const observerRef = useRef<IntersectionObserver | null>(null)
    // 一度でもハイライトしたメッセージのIDを記録（重複ハイライト防止）
    const highlightedOnceRef = useRef<Set<number>>(new Set())

    const msgs = useMemo(() => Array.isArray(messages) ? messages : [], [messages])

    // ハイライト対象のメッセージIDを計算（ステートレス）
    const highlightedIds = useMemo(() => {
        if (!joinTimestamp || !currentUser) return []

        return messages
            .filter(m => {
                // IDが存在し、他者のメッセージで、参加時刻より新しく、まだハイライトしていない
                return m.id != null &&
                    m.sender.id !== currentUser.id &&
                    m.createdAt &&
                    new Date(m.createdAt).getTime() > joinTimestamp &&
                    !highlightedOnceRef.current.has(m.id)
            })
            .map(m => m.id!)
    }, [messages, joinTimestamp, currentUser])

    // メッセージの高さを計算（可変サイズ対応）
    const getItemSize = useCallback((index: number) => {
        const message = messages[index]
        // メッセージの長さに応じて高さを計算（簡易的な実装）
        const contentLength = message.content?.length || 0
        const baseHeight = 100 // 基本の高さ
        const additionalHeight = Math.floor(contentLength / 50) * 20 // 50文字ごとに20px追加
        return baseHeight + additionalHeight
    }, [messages])

    // スクロール位置を最下部に移動
    const scrollToBottom = useCallback(() => {
        const list = listRef.current
        if (!list || !messages.length) return

        const scrollToIndex = messages.length - 1
        list.scrollToItem(scrollToIndex, "end")
    }, [messages])

    // Handle scroll to determine if at bottom
    const handleScroll = useCallback(({ scrollOffset }: { scrollOffset: number, scrollDirection: "forward" | "backward" }) => {
        // 自分のメッセージでスクロール中の場合は処理をスキップ
        if (isScrollingToBottomRef.current) {
            return
        }

        const list = listRef.current
        if (!list) return

        const listElement = list as unknown as { state: { scrollOffset: number }, props: { height: number } }
        const scrollHeight = msgs.reduce((total, _, index) => total + getItemSize(index), 0)
        const clientHeight = listElement.props.height
        const atBottom = scrollHeight - scrollOffset - clientHeight < 50

        setIsAtBottom(atBottom)

        if (atBottom && hasNewMessages) {
            setHasNewMessages(false)
        }
    }, [hasNewMessages, msgs, getItemSize])

    // 初回読み込み完了フラグ
    const initialLoadedRef = useRef(false)

    // 初回参加時刻を設定（初回読み込み時のみ）
    useEffect(() => {
        if (!initialLoadedRef.current && messages.length > 0 && joinTimestamp === null) {
            // 現在時刻を参加時刻として設定
            setJoinTimestamp(Date.now())
            scrollToBottom()
            initialLoadedRef.current = true
        }
    }, [messages.length, joinTimestamp, scrollToBottom])

    // 新着メッセージ検知とハイライト（自動スクロール用）
    const prevMessagesRef = useRef<ChatMessage[]>([])
    const isScrollingToBottomRef = useRef(false)

    useEffect(() => {
        // 参加時刻が設定されていない場合は処理しない
        if (!joinTimestamp) return

        // メッセージ数が変わっていない場合は処理しない
        if (messages.length === prevMessagesRef.current.length) {
            return
        }

        // 履歴読み込みの場合（先頭に追加）
        if (messages.length > prevMessagesRef.current.length &&
            prevMessagesRef.current.length > 0 &&
            messages[messages.length - 1].id === prevMessagesRef.current[prevMessagesRef.current.length - 1].id) {
            // 履歴読み込み時は prevMessagesRef を更新するだけで自動スクロール処理はしない
            prevMessagesRef.current = [...messages]
            return
        }

        // 新着メッセージを特定（末尾に追加された場合）
        const newMessages = messages.slice(prevMessagesRef.current.length)
        prevMessagesRef.current = [...messages]

        if (newMessages.length === 0) return

        // 自分メッセージと他者メッセージを分離
        const ownMessages = newMessages.filter(m => m.sender.id === currentUser?.id)
        const otherMessages = newMessages.filter(m => m.sender.id !== currentUser?.id)

        // 自分のメッセージがある場合は常に最下部にスクロール
        if (ownMessages.length > 0) {
            isScrollingToBottomRef.current = true
            requestAnimationFrame(() => {
                requestAnimationFrame(() => {
                    scrollToBottom()
                    setTimeout(() => {
                        isScrollingToBottomRef.current = false
                    }, 100)
                })
            })
            return
        }

        // 他者の新着メッセージがある場合
        if (otherMessages.length > 0) {
            if (isAtBottom) {
                // 最下部にいる場合は自動スクロール
                isScrollingToBottomRef.current = true
                requestAnimationFrame(() => {
                    requestAnimationFrame(() => {
                        scrollToBottom()
                        setTimeout(() => {
                            isScrollingToBottomRef.current = false
                        }, 100)
                    })
                })
            } else {
                // 画面外なら通知表示のみ
                setHasNewMessages(true)
            }
        }
    }, [messages, isAtBottom, currentUser, scrollToBottom, joinTimestamp])

    // IntersectionObserver初期化: 視界に入ったらハイライト済みとしてマーク
    useEffect(() => {
        observerRef.current = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const el = entry.target as HTMLElement
                    const id = Number(el.dataset.id)
                    // 視界に入ったらハイライト済みとしてマークし、一定時間後に重複防止リストに追加
                    setTimeout(() => {
                        highlightedOnceRef.current.add(id)
                    }, 3000) // 3秒間ハイライトを表示してからマーク
                    // 一度だけ監視
                    observerRef.current?.unobserve(el)
                }
            })
        }, { threshold: 1.0 })
        return () => observerRef.current?.disconnect()
    }, [])

    const onIntersect = useCallback(async (el: HTMLElement) => {
        if (!isLoadingRef.current) {
            const id = el.dataset.id
            if (!id) {
                return
            }
            isLoadingRef.current = true

            // 現在の最後のメッセージIDを記録（履歴読み込み判定用）
            const lastMessageId = messages.length > 0 ? messages[messages.length - 1].id?.toString() || null : null

            // prepare adjust info
            pendingAdjustRef.current = {
                targetId: id,
                initialCount: messages.length,
                lastMessageId
            }

            try {
                await loadOlderMessages()
            } catch {
                // ignore
            }
            // do not scroll here; adjust in effect after messages update
        }
    }, [loadOlderMessages, messages.length])

    // adjust scroll when messages load
    useEffect(() => {
        const info = pendingAdjustRef.current
        if (info && messages.length > info.initialCount) {
            // 履歴読み込みかどうかを判定
            // 最後のメッセージIDが変わっていない = 新しいメッセージがない = 古いメッセージのみ追加
            const currentLastMessage = messages.length > 0 ? messages[messages.length - 1] : null
            const currentLastMessageId = currentLastMessage?.id?.toString() || null
            const isHistoryLoad = currentLastMessageId === info.lastMessageId

            if (isHistoryLoad) {
                // 古いメッセージのみが追加された場合のみスクロール調整を実行
                const idx = messages.findIndex(m => m.id?.toString() === info.targetId)
                if (idx !== -1 && listRef.current) {
                    listRef.current.resetAfterIndex(0, true)
                    // 一アイテム分ずらして表示
                    const scrollIndex = Math.max(0, idx - loadTriggerCount)
                    listRef.current.scrollToItem(scrollIndex, 'start')
                }
            } else {
                // 新しいメッセージも追加された場合は、リセットのみでスクロール調整はしない
                if (listRef.current) {
                    listRef.current.resetAfterIndex(0, true)
                }
            }

            pendingAdjustRef.current = null
            isLoadingRef.current = false
        }
    }, [messages, loadTriggerCount])

    // コンテナの高さを計算
    const [containerHeight, setContainerHeight] = useState(0)
    const containerRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        const updateHeight = () => {
            if (containerRef.current) {
                setContainerHeight(containerRef.current.clientHeight)
            }
        }
        updateHeight()
        window.addEventListener('resize', updateHeight)
        return () => window.removeEventListener('resize', updateHeight)
    }, [])

    return (
        <div className="flex-1 min-h-0 relative" ref={containerRef}>
            <List
                ref={listRef}
                height={containerHeight}
                itemCount={msgs.length}
                itemSize={getItemSize}
                width="100%"
                onScroll={handleScroll}
                onItemsRendered={({ visibleStopIndex }: { visibleStopIndex: number }) => {
                    // onItemsRenderedでのisAtBottom更新は削除して、handleScrollに任せる
                    // これにより状態更新の競合を防ぐ
                }}
                itemData={{
                    messages: msgs,
                    highlightedIds,
                    fadeInTime,
                    fadeOutTime,
                    loadTriggerCount,
                    topRef,
                    observerRef,
                    onIntersect
                }}
            >
                {MessageItem}
            </List >
            {hasNewMessages && (
                <button
                    onClick={() => {
                        scrollToBottom()
                        setHasNewMessages(false)
                    }}
                    className="absolute bottom-4 left-1/2 transform -translate-x-1/2 bg-blue-100 text-blue-600 px-4 py-2 rounded-full shadow"
                >
                    新着メッセージ
                </button>
            )
            }
        </div >
    )
}
