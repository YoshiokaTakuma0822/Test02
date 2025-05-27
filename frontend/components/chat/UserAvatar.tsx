"use client"

import { HoverPopover } from "@/components/ui/hover-popover"
import { User } from "@/types/chat"

interface UserAvatarProps {
    user: User | null
}

export function UserAvatar({ user }: UserAvatarProps) {
    if (!user) {
        return null
    }

    const getInitials = (name: string) => {
        return name
            .split(" ")
            .map(part => part.charAt(0))
            .join("")
            .toUpperCase()
            .slice(0, 2)
    }

    const popoverContent = (
        <div className="space-y-3">
            <div className="flex items-center space-x-3">
                <div className="w-12 h-12 bg-blue-500 text-white rounded-full flex items-center justify-center font-semibold text-lg">
                    {getInitials(user.name)}
                </div>
                <div>
                    <p className="font-semibold text-gray-900">{user.name}</p>
                    <p className="text-sm text-gray-500">一時ユーザー</p>
                </div>
            </div>

            <div className="border-t pt-3 space-y-2">
                <div className="text-sm">
                    <span className="text-gray-600">ユーザーID: </span>
                    <span className="font-mono text-gray-900">{user.id || 'unknown'}</span>
                </div>
                {user.email && (
                    <div className="text-sm">
                        <span className="text-gray-600">メール: </span>
                        <span className="text-gray-900">{user.email}</span>
                    </div>
                )}
            </div>

            <div className="border-t pt-3">
                <button
                    className="w-full text-sm text-gray-500 hover:text-gray-700 py-2 px-3 rounded border border-gray-200 hover:bg-gray-50 transition-colors"
                    disabled
                >
                    ログイン（未実装）
                </button>
            </div>
        </div>
    )

    return (
        <HoverPopover
            content={popoverContent}
            align="end"
            className="w-80 p-3"
        >
            <div className="w-10 h-10 bg-blue-500 text-white rounded-full flex items-center justify-center font-semibold cursor-pointer hover:bg-blue-600 transition-colors">
                {getInitials(user.name)}
            </div>
        </HoverPopover>
    )
}
