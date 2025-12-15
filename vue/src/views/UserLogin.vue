<template>
  <div class="login-container">
    <div class="login-box">
      <!-- 表单容器 -->
      <div class="login-box-form">
        <el-form ref="formRef" :rules="data.rules" :model="data.form" label-width="0">
          <!-- 标题 -->
          <div class="login-title">欢迎登录</div>

          <!-- 账号输入框 -->
          <el-form-item prop="username">
            <el-input
              size="large"
              v-model="data.form.username"
              placeholder="请输入账号（6-20位字符）"
              prefix-icon="User"
              autocomplete="username"
              clearable
            ></el-input>
          </el-form-item>

          <!-- 密码输入框 -->
          <el-form-item prop="password">
            <el-input
              size="large"
              v-model="data.form.password"
              placeholder="请输入密码（至少6位）"
              prefix-icon="Lock"
              show-password
              clearable
            ></el-input>
          </el-form-item>

          <!-- 记住密码选项 -->
          <el-form-item class="remember-option">
            <el-checkbox v-model="rememberMe" size="small"> 记住密码（7天内自动登录） </el-checkbox>
          </el-form-item>

          <!-- 登录注册按钮 -->
          <el-form-item>
            <div class="btn-group">
              <el-button
                @click="login"
                size="large"
                class="login-btn"
                :loading="isLoading"
                :disabled="isLoading"
              >
                <template  v-if="isLoading">登录中</template >
                <template  v-else>登 录</template >
              </el-button>
            </div>
          </el-form-item>
          <el-button type="link" class="register-link" @click="goToRegister"
            >没有账号？去注册</el-button
          >
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import router from '@/router/index.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import { getPublicIp } from '@/utils/ipUtils.js'
import { useUserProfileStore } from '@/store/userProfile.js'
import { checkLogin, validatePass, validateUsername } from '@/utils/commonUtils.js'

const userProfile = useUserProfileStore()

// 加载状态（防止重复提交）
const isLoading = ref(false)
// 记住密码开关
const rememberMe = ref(false)

const data = reactive({
  form: {
    username: '',
    password: '',
    lastLoginIp: 'unknown',
  },
  rules: {
    username: [
      {
        validator: validateUsername,
        required: true,
        trigger: 'blur',
      },
    ],
    password: [
      {
        validator: validatePass,
        required: true,
        trigger: 'blur',
      },
    ],
  },
})

const formRef = ref()

const login = async () => {
  if (checkLogin(userProfile)) {
    ElMessage.warning('已登录，请勿重复操作')
    return
  }
  formRef.value.validate(async (valid) => {
    if (valid) {
      isLoading.value = true
      try {
        const response = await request('post', '/login', data.form)
        if (response.code === 200) {
          // 存储用户信息和token
          userProfile.setUserProfile(response.data)
          userProfile.setUserToken(response.data.token)

          // 记住密码（7天有效期）
          if (rememberMe.value) {
            const expires = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()
            userProfile.setRememberMe(data.form.username, data.form.password, expires)
          }

          ElMessage.success('登录成功，跳转至首页')
          setTimeout(() => {
            router.push('/home')
          }, 500)
        } else {
          ElMessage.error(response.msg || '登录失败，请重试')
          data.form.password = ''
        }
      } catch (error) {
        ElMessage.error(error)
        data.form.password = ''
      } finally {
        isLoading.value = false
      }
    }
  })
}

onMounted(async () => {
  // 读取记住的密码
  if (userProfile.rememberMe.isRemember) {
    try {
      if (new Date(userProfile.rememberMe.expires) > new Date()) {
        data.form.username = userProfile.rememberMe.username
        data.form.password = userProfile.rememberMe.password
        rememberMe.value = true
      } else {
        userProfile.clearRememberMe()
      }
    } catch (e) {
      console.error('解析记住的用户信息失败:', e)
      userProfile.clearRememberMe()
    }
  }

  // 获取公网IP（处理失败情况）
  try {
    const ip = await getPublicIp()
    if (ip) data.form.lastLoginIp = ip
  } catch (e) {
    console.warn('获取IP失败，使用默认值:', e)
  }
})

// 跳转注册页面
const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
/* 全局背景 */
.login-container {
  width: 100%;
  height: 100vh;
  background-image: url('@/assets/images/login_bg.jpg');
  background-size: cover;
  position: relative;
}

/* 表单容器 */
.login-box {
  width: 600px;
  height: 520px;
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  margin: auto;

  background: #ffffff;
  border-radius: 14px;
  border: 1px solid rgba(200, 200, 200, 0.2);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.1);

  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  box-sizing: border-box;
}

/* 表单容器 */
.login-box-form {
  width: 65%;
  padding: 20px 0;
  max-width: 350px;
}

/* 标题样式 */
.login-title {
  margin-top: 10px;
  margin-bottom: 60px;
  font-size: 32px;
  color: deepskyblue;
  text-align: center;
  font-weight: 500;
}

/* 统一输入框相关样式 */
::v-deep .el-input,
::v-deep .el-input__wrapper,
::v-deep .el-input__inner {
  border-radius: 10px !important;
  box-sizing: border-box !important;
}

::v-deep .el-input {
  width: 80%;
  margin-left: auto !important;
  margin-right: auto !important;
  margin-bottom: 16px;
}

::v-deep .el-input__prefix {
  padding-left: 10px;
}

::v-deep .el-input__suffix {
  padding-right: 10px;
}

::v-deep .el-input__suffix-inner {
  display: flex;
  gap: 6px;
}

::v-deep .el-input__wrapper,
::v-deep .el-input__inner {
  border-radius: 10px !important;
  box-sizing: border-box !important;
  background: #f5f7fa;
}

/* 输入框样式 */
::v-deep .el-input__inner {
  background: #f5f7fa;
  padding: 14px 18px;
  width: 100% !important;
  color: #1e1e1e;
  font-size: 14px;
  padding-left: 10px !important;
  padding-right: 10px !important;
}

/* 输入框聚焦效果 */
::v-deep .el-input__wrapper {
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
}

::v-deep .el-input:focus-within {
  width: 80% !important;
  box-shadow: 0 0 0 3px rgba(0, 191, 255, 0.2);
  border-radius: 10px !important;
  border: none !important;
  padding: 0 !important;
}

::v-deep .el-input__inner::placeholder {
  color: #909399;
}

/* 错误提示样式 */
::v-deep .el-form-item__error {
  margin-top: -15px !important;
  font-size: 12px;
  padding-left: 12%;
}

/* 记住密码选项样式 */
.remember-option {
  width: 80%;
  margin: -16px auto 26px;
  text-align: left;
  padding-left: 16px !important;
}

::v-deep .remember-option .el-checkbox {
  color: #888;
  font-size: 12px;
}

/* 按钮组 */
.btn-group {
  display: flex;
  gap: 14px;
  width: 80%;
  margin-top: 20px;
  margin-bottom: 12px;
  margin-left: auto !important;
  margin-right: auto !important;
}

/* 登录按钮样式 */
.login-btn {
  flex: 1;
  color: #fff;
  background-color: deepskyblue;
  font-size: 16px;
  padding: 12px 0;
  border-radius: 10px;
  border: none;
}

.login-btn:hover {
  background: #00bfffcc;
  color: #fff;
}

/* 注册跳转文本样式 */
::v-deep .register-link {
  display: block;
  width: 80%;
  text-align: center;
  color: deepskyblue;
  font-size: 13px;
  cursor: pointer;
  margin-top: 20px;
  margin-bottom: 5px;
  margin-left: auto !important;
  margin-right: auto !important;
  transition: color 0.2s;
  padding: 4px 0 !important;
  background: transparent !important;
  border: none !important
}

::v-deep .register-link:hover {
  color: #0080ff !important;
  text-decoration: underline;
  background: transparent !important;
}
</style>
