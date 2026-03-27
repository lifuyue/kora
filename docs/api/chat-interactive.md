# Chat Interactive

## 目标
支持工作流中需要用户继续参与的节点，典型类型为 `userSelect` 和 `userInput`。

## 交互模式
- `userSelect`: 服务端给出候选按钮，用户点击后继续流程。
- `userInput`: 服务端声明表单或文本输入需求，用户填写后提交。

## 客户端要求
- 交互节点必须绑定到所属消息与流程上下文。
- 用户输入后继续调用聊天接口或专用续交流程接口。
- 未完成的交互节点需要可恢复，避免应用切后台后丢失。

## 上游实现模式参考
- FastGPT 把交互节点定义为工作流中的正式节点类型，而不是异常分支，因此 Kora 要把 `userSelect` 和 `userInput` 当成消息流的一等结构。
- 详见 [../reference/fastgpt-implementation-patterns.md](../reference/fastgpt-implementation-patterns.md)。
