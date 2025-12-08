<template>
  <div class="login-container">
    <div class="login-box">
      <!-- 表单容器：用于统一居中控制 -->
      <div class="login-box-form">
        <el-form ref="formRef" :rules="data.rules" :model="data.form" label-width="0px">
          <!-- 标题 -->
          <div class="register-title">欢迎注册</div>

          <!-- 账号输入框 -->
          <el-form-item prop="username">
            <el-input
              size="large"
              v-model="data.form.username"
              placeholder="请输入账号"
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
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              clearable
            ></el-input>
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              size="large"
              v-model="data.form.confirmPassword"
              placeholder="请再次输入密码"
              prefix-icon="Lock"
              show-password
              clearable
            ></el-input>
          </el-form-item>

          <!-- 注册按钮 -->
          <el-form-item>
            <div class="btn-group">
              <el-button @click="register" size="large" class="register-btn">注 册</el-button>
            </div>
          </el-form-item>
          <!-- 已有账号跳转登录 -->
          <el-button type="text" class="login-link" @click="goToLogin">已有账号？去登录</el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import router from '@/router/index.js'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import { validatePass, validateUsername } from '@/utils/commonUtils.js'

const validateConfirmPass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次确认密码'))
  } else if (value !== data.form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const data = reactive({
  form: {
    username: '',
    phone: '',
    password: '',
    confirmPassword: '',
  },
  rules: {
    username: [{ validator: validateUsername, required: true, trigger: 'blur' }],
    password: [{ validator: validatePass, required: true, trigger: 'blur' }],
    confirmPassword: [{ validator: validateConfirmPass, trigger: 'blur' }],
  },
})

const formRef = ref()

const register = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      const registerData = {
        username: data.form.username,
        phone: data.form.phone,
        password: data.form.password,
      }

      const response = await request('post', '/register', registerData)
      if (response.code === 200) {
        ElMessage.success('注册成功，已跳转登录页')
        setTimeout(() => {
          router.push('/login')
        }, 500)
      } else {
        ElMessage.error(response.msg)
      }
    }
  })
}

// 跳转登录页面
const goToLogin = () => {
  router.push('/login')
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
.register-title {
  margin-top: 20px;
  margin-bottom: 50px;
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
  margin-bottom: 10px;
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

::v-deep .el-form-item__error {
  margin-top: -5px !important;
  font-size: 12px;
  padding-left: 12%;
}

/* 按钮组 */
.btn-group {
  display: flex;
  width: 80%;
  margin-top: 30px;
  margin-bottom: 10px;
  margin-left: auto !important;
  margin-right: auto !important;
}

/* 注册按钮样式 */
.register-btn {
  flex: 1;
  color: #fff;
  background-color: deepskyblue;
  font-size: 16px;
  padding: 12px 0;
  border-radius: 10px;
}

.register-btn:hover {
  background: #00bfffcc;
  color: #fff;
}

/* 登录跳转文本样式 */
::v-deep .login-link {
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
}

::v-deep .login-link:hover {
  color: #0080ff !important;
  text-decoration: underline;
  background: transparent !important;
}
</style>
