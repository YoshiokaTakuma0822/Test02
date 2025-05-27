"use client"

import { cn } from "@/lib/utils"
import { Anchor, Content, Portal, Root } from "@radix-ui/react-popover"
import { useEffect, useRef, useState } from "react"

interface HoverPopoverProps {
    children: React.ReactNode
    content: React.ReactNode
    className?: string
    align?: "start" | "center" | "end"
    side?: "top" | "right" | "bottom" | "left"
    sideOffset?: number
}

export function HoverPopover({
    children,
    content,
    className,
    align = "end",
    side = "bottom",
    sideOffset = 8,
}: HoverPopoverProps) {
    const [open, setOpen] = useState(false)
    const [isPinned, setIsPinned] = useState(false)
    const [isHoveringTrigger, setIsHoveringTrigger] = useState(false)
    const [isHoveringContent, setIsHoveringContent] = useState(false)
    const timeoutRef = useRef<NodeJS.Timeout | null>(null)
    const popoverRef = useRef<HTMLDivElement>(null)

    const handleMouseEnter = () => {
        if (!isPinned) {
            setIsHoveringTrigger(true)
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current)
                timeoutRef.current = null
            }
            setOpen(true)
        }
    }

    const handleMouseLeave = () => {
        if (!isPinned) {
            setIsHoveringTrigger(false)
            timeoutRef.current = setTimeout(() => {
                if (!isHoveringContent && !isPinned) {
                    setOpen(false)
                }
            }, 150)
        }
    }

    const handleContentMouseEnter = () => {
        if (!isPinned) {
            setIsHoveringContent(true)
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current)
                timeoutRef.current = null
            }
        }
    }

    const handleContentMouseLeave = () => {
        if (!isPinned) {
            setIsHoveringContent(false)
            timeoutRef.current = setTimeout(() => {
                if (!isHoveringTrigger && !isPinned) {
                    setOpen(false)
                }
            }, 150)
        }
    }

    const handleClick = (event: React.MouseEvent) => {
        event.preventDefault() // デフォルトの動作を防ぐ

        // クリックはピン留め状態のトグルのみ
        setIsPinned(!isPinned)

        // ピンを解除した場合、自動的に閉じる
        if (isPinned) {
            setOpen(false)
        } else {
            // ピン留めした場合、開いていなければ開く
            if (!open) {
                setOpen(true)
            }
        }
    }

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (isPinned && popoverRef.current && !popoverRef.current.contains(event.target as Node)) {
                // ポップオーバーの外部をクリックした場合、ピン留めを解除
                const triggerElement = event.target as Element
                if (!triggerElement.closest('[data-popover-trigger]')) {
                    setOpen(false)
                    setIsPinned(false)
                }
            }
        }

        if (isPinned) {
            document.addEventListener('mousedown', handleClickOutside)
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside)
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current)
            }
        }
    }, [isPinned])

    // ピン解除時の自動クローズ機能
    useEffect(() => {
        if (!isPinned && !isHoveringTrigger && !isHoveringContent) {
            // ピンが解除され、マウスも離れている場合は閉じる
            const timer = setTimeout(() => {
                setOpen(false)
            }, 150)

            return () => clearTimeout(timer)
        }
    }, [isPinned, isHoveringTrigger, isHoveringContent])

    return (
        <Root
            open={open}
            onOpenChange={() => {
                // Radix UIの自動制御を無効化
                // 我々が完全に制御する
            }}
        >
            <Anchor asChild>
                <div
                    data-popover-trigger
                    onMouseEnter={handleMouseEnter}
                    onMouseLeave={handleMouseLeave}
                    onClick={handleClick}
                    style={{ display: 'inline-block' }}
                >
                    {children}
                </div>
            </Anchor>
            <Portal>
                <Content
                    ref={popoverRef}
                    align={align}
                    side={side}
                    sideOffset={sideOffset}
                    onMouseEnter={handleContentMouseEnter}
                    onMouseLeave={handleContentMouseLeave}
                    className={cn(
                        "bg-white text-gray-900 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 rounded-md border border-gray-200 shadow-lg outline-hidden",
                        // ピン留め状態の視覚的フィードバック
                        isPinned && "ring-2 ring-blue-500 ring-opacity-20",
                        className
                    )}
                >
                    {/* ピン留め状態のインジケーター */}
                    {isPinned && (
                        <div className="absolute -top-1 -right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-white" />
                    )}
                    {content}
                </Content>
            </Portal>
        </Root>
    )
}
