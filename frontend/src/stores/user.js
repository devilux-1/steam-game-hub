// 用户会话状态：保存当前登录的 Steam 用户，供顶部导航与「我的游戏库」页共用。
import { defineStore } from 'pinia'
import { getMe, logout as logoutApi } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    /** 当前用户对象，未登录为 null */
    user: null,
    /** 是否正在请求 /api/me */
    loading: false,
    /** 是否已完成过一次用户信息拉取，避免重复请求 */
    loaded: false
  }),

  getters: {
    /** 是否已登录 */
    isLogin: (state) => Boolean(state.user),
    /** 顶部展示用的昵称，缺昵称时退回 SteamID */
    displayName: (state) =>
      state.user ? state.user.personaName || state.user.steamId || 'Steam 用户' : ''
  },

  actions: {
    /**
     * 拉取当前登录用户，失败按未登录处理。
     * 请求参数：无
     * 返回参数：无；结果写入 user
     */
    async fetchMe() {
      this.loading = true
      try {
        this.user = await getMe()
      } catch {
        // 后端不可用或接口异常时按未登录展示，不阻塞页面
        this.user = null
      } finally {
        this.loading = false
        this.loaded = true
      }
    },

    /**
     * 退出登录并清空本地状态。
     * 请求参数：无
     * 返回参数：无；即使接口失败也会清空本地状态
     */
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.user = null
      }
    }
  }
})
