import type { InjectionKey } from 'vue'
import type { Emitter, EventType } from 'mitt'

export const eventEmitter = Symbol() as InjectionKey<Emitter<Record<EventType, unknown>>>
