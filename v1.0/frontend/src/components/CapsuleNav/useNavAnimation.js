import gsap from 'gsap'
import { onUnmounted } from 'vue'

/**
 * Logo 入场动画 —— 划入 + 旋转
 * @param {import('vue').Ref<HTMLElement|null>} logoRef
 * @param {string} color 主题色（用于文字颜色过渡）
 */
export function useLogoAnimation(logoRef, color = '#6b5a4b') {
  let tween = null

  function playEnter() {
    if (!logoRef.value) return
    tween = gsap.fromTo(
      logoRef.value,
      { x: -36, rotation: -90, opacity: 0 },
      { x: 0, rotation: 0, opacity: 1, duration: 0.8, ease: 'back.out(1.4)' }
    )
  }

  function playHover() {
    if (!logoRef.value) return
    gsap.to(logoRef.value, {
      rotation: 12,
      scale: 1.08,
      duration: 0.35,
      ease: 'power2.out',
    })
  }

  function playHoverLeave() {
    if (!logoRef.value) return
    gsap.to(logoRef.value, {
      rotation: 0,
      scale: 1,
      duration: 0.35,
      ease: 'power2.out',
    })
  }

  onUnmounted(() => {
    if (tween) tween.kill()
    gsap.killTweensOf(logoRef.value)
  })

  return { playEnter, playHover, playHoverLeave }
}

/**
 * 导航项悬浮圆形扩散 + 双层文字位移动效
 *   - 圆形遮罩 scale(0) → scale(2.5)，带阴影晕染
 *   - 深色文字向上滑出 + 白色文字从下方滑入（位移切换）
 * @param {import('vue').Ref<HTMLElement|null>} itemRef       导航项容器
 * @param {import('vue').Ref<HTMLElement|null>} circleRef     扩散圆形
 * @param {import('vue').Ref<HTMLElement|null>} textOuterRef  默认深色文字层
 * @param {import('vue').Ref<HTMLElement|null>} textInnerRef  悬浮白色文字层
 * @param {string} color 主题色
 */
export function useNavItemAnimation(itemRef, circleRef, textOuterRef, textInnerRef, color = '#6b5a4b') {
  let enterTween = null
  let leaveTween = null

  function onEnter() {
    if (!circleRef.value || !textOuterRef.value || !textInnerRef.value) return
    leaveTween?.kill()
    enterTween = gsap.timeline({ overwrite: 'auto' })

    // 圆形扩散 + 淡入 + 微弱阴影晕染
    enterTween.to(circleRef.value, {
      scale: 2.5,
      opacity: 1,
      boxShadow: `0 0 18px ${color}44`,
      duration: 0.38,
      ease: 'power3.out',
    }, 0)

    // 深色文字向上滑出
    enterTween.to(textOuterRef.value, {
      y: -12,
      opacity: 0,
      duration: 0.2,
      ease: 'power2.in',
    }, 0)

    // 白色文字从下方滑入（略微延迟产生接力感）
    enterTween.to(textInnerRef.value, {
      y: 0,
      opacity: 1,
      duration: 0.28,
      ease: 'power2.out',
    }, 0.04)
  }

  function onLeave() {
    if (!circleRef.value || !textOuterRef.value || !textInnerRef.value) return
    enterTween?.kill()
    leaveTween = gsap.timeline({ overwrite: 'auto' })

    // 白色文字先滑回下方
    leaveTween.to(textInnerRef.value, {
      y: '100%',
      opacity: 0,
      duration: 0.18,
      ease: 'power2.in',
    }, 0)

    // 圆形缩回 + 阴影消失
    leaveTween.to(circleRef.value, {
      scale: 0,
      opacity: 0,
      boxShadow: `0 0 0px ${color}00`,
      duration: 0.32,
      ease: 'power3.in',
    }, 0.02)

    // 深色文字从上方滑回
    leaveTween.to(textOuterRef.value, {
      y: 0,
      opacity: 1,
      duration: 0.25,
      ease: 'power2.out',
    }, 0.06)
  }

  onUnmounted(() => {
    enterTween?.kill()
    leaveTween?.kill()
  })

  return { onEnter, onLeave }
}

/**
 * 汉堡图标变形动画（两条横线 → ✕）
 *   上线顺时针旋转 45° + 下移 → 下线逆时针旋转 -45° + 上移
 *   两条线交叉形成 X，过渡顺滑
 * @param {import('vue').Ref<Array<HTMLElement|null>>} lineRefs [topLine, botLine]
 * @returns {{ toggle: (isOpen: boolean) => void }}
 */
export function useHamburgerAnimation(lineRefs) {
  let timeline = null

  function toggle(isOpen) {
    if (!lineRefs.value || lineRefs.value.length < 2) return
    const [top, bot] = lineRefs.value
    timeline?.kill()

    // gap: 7px → 每条线移动位移 = 3.5px（移动到对方位置形成 X 交叉）
    const offset = 3.5

    if (isOpen) {
      timeline = gsap.timeline()
      timeline.to(top, {
        rotation: 45,
        y: offset,
        duration: 0.3,
        ease: 'power2.out',
      }, 0)
      timeline.to(bot, {
        rotation: -45,
        y: -offset,
        duration: 0.3,
        ease: 'power2.out',
      }, 0)
    } else {
      timeline = gsap.timeline()
      timeline.to(top, {
        rotation: 0,
        y: 0,
        duration: 0.28,
        ease: 'power2.out',
      }, 0)
      timeline.to(bot, {
        rotation: 0,
        y: 0,
        duration: 0.28,
        ease: 'power2.out',
      }, 0)
    }
  }

  onUnmounted(() => {
    timeline?.kill()
  })

  return { toggle }
}

/**
 * 移动端折叠菜单 —— 顶部原点缩放 + 渐变位移 + 阴影悬浮弹窗
 *   打开：从顶部中心 scaleY(0) 纵向放大展开，同时渐显 + 微下移 + 阴影增强
 *   子项：面板展开到一半后从上方错位滑入
 *   关闭：子项先收 → 面板缩放回顶部原点
 * @param {import('vue').Ref<HTMLElement|null>} panelRef   菜单面板
 * @param {import('vue').Ref<Array<HTMLElement|null>>} itemRefs 菜单子项
 * @param {string} color 主题色（用于阴影边框）
 */
export function useMobileMenuAnimation(panelRef, itemRefs, color = '#6b5a4b') {
  let openTL = null
  let closeTL = null

  function open() {
    if (!panelRef.value) return
    closeTL?.kill()
    openTL = gsap.timeline({ defaults: { overwrite: 'auto' } })

    // 面板从顶部中心纵向展开 + 渐显 + 微位移 + 阴影渐强
    openTL.fromTo(
      panelRef.value,
      {
        scaleY: 0,
        scaleX: 0.75,
        opacity: 0,
        y: -16,
        transformOrigin: 'top center',
        boxShadow: '0 0 0 rgba(0,0,0,0), 0 0 0 0 rgba(0,0,0,0)',
      },
      {
        scaleY: 1,
        scaleX: 1,
        opacity: 1,
        y: 0,
        duration: 0.48,
        ease: 'power3.out',
        boxShadow: `0 18px 50px rgba(0,0,0,0.14), 0 0 0 1px ${color}22`,
      },
      0
    )

    // 子项从上方错位滑入（面板展开 60% 后触发）
    if (itemRefs.value && itemRefs.value.length) {
      const items = itemRefs.value.filter(Boolean)
      openTL.fromTo(
        items,
        { y: -12, opacity: 0 },
        { y: 0, opacity: 1, duration: 0.3, stagger: 0.06, ease: 'power2.out' },
        0.18
      )
    }
  }

  function close() {
    if (!panelRef.value) return
    openTL?.kill()
    const items = itemRefs.value?.filter(Boolean) || []

    closeTL = gsap.timeline({ defaults: { overwrite: 'auto' } })

    // 子项先向上收起
    if (items.length) {
      closeTL.to(items, {
        y: -10, opacity: 0, duration: 0.2, stagger: 0.04, ease: 'power2.in',
      }, 0)
    }

    // 面板缩放回收至顶部原点 + 阴影消失
    closeTL.to(
      panelRef.value,
      {
        scaleY: 0,
        scaleX: 0.75,
        opacity: 0,
        y: -16,
        duration: 0.38,
        ease: 'power3.in',
        transformOrigin: 'top center',
        boxShadow: '0 0 0 rgba(0,0,0,0), 0 0 0 0 rgba(0,0,0,0)',
      },
      0.06
    )
  }

  onUnmounted(() => {
    openTL?.kill()
    closeTL?.kill()
  })

  return { open, close }
}

/**
 * 整体入场动画 —— 胶囊条 + Logo + 导航项依次出场
 * @param {import('vue').Ref<HTMLElement|null>} navRef
 * @param {import('vue').Ref<HTMLElement|null>} logoRef
 * @param {import('vue').Ref<Array<HTMLElement|null>>} itemRefs
 */
export function useEntranceAnimation(navRef, logoRef, itemRefs) {
  let tl = null

  function play() {
    if (!navRef.value || !logoRef.value) return
    tl = gsap.timeline({ defaults: { overwrite: 'auto' } })
    // 1. 胶囊条纵向展开
    tl.fromTo(navRef.value, { scaleY: 0 }, { scaleY: 1, duration: 0.5, ease: 'power3.out' })
    // 2. Logo 旋转滑入
    tl.fromTo(
      logoRef.value,
      { x: -36, rotation: -90, opacity: 0 },
      { x: 0, rotation: 0, opacity: 1, duration: 0.7, ease: 'back.out(1.4)' },
      '-=0.25'
    )
    // 3. 导航项从左到右错位入场
    const items = itemRefs.value?.filter(Boolean) || []
    if (items.length) {
      tl.fromTo(
        items,
        { x: 15, opacity: 0 },
        { x: 0, opacity: 1, duration: 0.35, stagger: 0.08, ease: 'power2.out' },
        '-=0.1'
      )
    }
  }

  onUnmounted(() => {
    tl?.kill()
  })

  return { play }
}
