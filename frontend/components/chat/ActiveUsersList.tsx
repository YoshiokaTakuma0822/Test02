import { useChatContext } from "@/lib/ChatContext"

export function ActiveUsersList() {
    const { activeUsers } = useChatContext()

    return (
        <div className="bg-gray-50 p-4 mb-4 border-b">
            <h2 className="text-lg font-semibold mb-2">参加者一覧</h2>
            <div className="flex flex-wrap gap-2">
                {activeUsers.map((user, i) => (
                    <span
                        key={user.id || i}
                        className="bg-white px-3 py-1 rounded-full text-sm border border-gray-200"
                    >
                        {user.name}
                    </span>
                ))}
            </div>
        </div>
    )
}
