import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { useChatContext } from "@/lib/ChatContext"
import { FormEvent, useState } from "react"

export function ChatInput() {
    const [message, setMessage] = useState("")
    const { sendMessage } = useChatContext()

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault()
        send()
    }

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault()
            send()
        }
    }

    const send = () => {
        if (message.trim()) {
            sendMessage(message)
            setMessage("")
        }
    }

    return (
        <form onSubmit={handleSubmit} className="p-4 bg-white border-t shadow-lg relative z-10">
            <div className="flex gap-4">
                <Textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="メッセージを入力...（Enterで送信、Shift + Enterで改行）"
                    className="flex-1 min-h-[60px] max-h-[200px]"
                />
                <Button type="submit" className="self-end">
                    送信
                </Button>
            </div>
        </form>
    )
}
