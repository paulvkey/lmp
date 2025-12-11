// 跳转登录页面
export const goToLogin = (router) => {
  router.push('/login')
}

// 跳转注册页面
export const goToRegister = (router) => {
  router.push('/register')
}

export const validateUsername = (rule, value, callback) => {
  const usernameReg = /^[a-zA-Z][a-zA-Z0-9]{3,19}$/
  if (!value) {
    callback(new Error('请输入账号'))
  } else if (!usernameReg.test(value)) {
    callback(new Error('账号必须包含字母或数字，首位为字母[4~20]'))
  } else {
    callback()
  }
}

export const validatePhone = (rule, value, callback) => {
  const phoneReg = /^1[3-9]\d{9}$/
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!phoneReg.test(value)) {
    callback(new Error('请输入正确的手机号格式'))
  } else {
    callback()
  }
}

export const checkPhone = (rule, value, callback) => {
  const phoneReg = /^1[3-9]\d{9}$/
  if (value && !phoneReg.test(value)) {
    callback(new Error('请输入正确的手机号格式'))
  } else {
    callback()
  }
}

export const validatePass = (rule, value, callback) => {
  const passwordReg = /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (!passwordReg.test(value)) {
    callback(new Error('密码必须包含字母和数字[6~20]'))
  } else {
    callback()
  }
}

export const throttle = (fn, delay = 100) => {
  let lastTime = 0
  return (...args) => {
    const now = Date.now()
    if (now - lastTime > delay) {
      fn.apply(this, args)
      lastTime = now
    }
  }
}

export const checkLogin = (userProfile) => {
  return userProfile.isLogin && userProfile.userId > 0
}

export const escapeMsg = (content) => {
  if (!content) return ''
  // 仅转义 HTML 特殊字符
  let escaped = content
    .replace(/&/g, '&amp;') // & → &amp;
    .replace(/</g, '&lt;') // < → &lt;
    .replace(/>/g, '&gt;') // > → &gt;
    .replace(/"/g, '&quot;') // " → &quot;
    .replace(/'/g, '&#039;') // ' → &#039;
  // 处理空白字符（空格、制表符、换行）
  escaped = escaped
    .replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') // 制表符 → 4个空格
    .replace(/ /g, '&nbsp;') // 空格 → 非换行空格
    .replace(/\n/g, '<br>') // 换行 → <br>
  return escaped
}
