<template>
  <div class="profile-container">
    <!-- 页面标题 -->
    <div class="profile-header">
      <h1>个人中心</h1>
    </div>

    <!-- 主内容区 -->
    <div class="profile-content">
      <!-- 左侧导航 -->
      <el-menu
        class="profile-menu"
        default-active="profile"
        mode="vertical"
        @select="handleMenuSelect"
      >
        <el-menu-item index="profile">
          <el-icon><User /></el-icon>
          <span>个人信息</span>
        </el-menu-item>
        <el-menu-item index="security">
          <el-icon><Lock /></el-icon>
          <span>账号安全</span>
        </el-menu-item>
      </el-menu>

      <!-- 右侧设置内容 -->
      <div class="profile-form">
        <!-- 个人信息设置 -->
        <el-form
          v-if="activeTab === 'profile'"
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="140px"
          class="setting-form-item"
        >
          <!-- 头像区域 -->
          <el-form-item label="头像">
            <div class="avatar-container">
              <div class="avatar-wrapper">
                <div class="avatar-preview">
                  <img
                    :src="profileForm.avatar"
                    alt="用户头像"
                    class="user-avatar"
                    ref="avatarRef"
                  />

                  <div class="custom-loading" v-if="isAvatarUploading">
                    <div class="loading-spinner"></div>
                  </div>
                </div>
                <input
                  type="file"
                  ref="avatarInputRef"
                  class="avatar-input"
                  accept="image/*"
                  @change="handleAvatarChange"
                />
                <div
                  class="avatar-upload-overlay"
                  @click="triggerAvatarUpload"
                  v-if="isProfileEditing"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="12"
                    height="12"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="17 8 12 3 7 8"></polyline>
                    <line x1="12" y1="3" x2="12" y2="15"></line>
                  </svg>
                </div>
              </div>
              <!-- 头像上传提示 -->
              <div class="avatar-tip" v-if="isProfileEditing">支持JPG、PNG格式，大小不超过2MB</div>
            </div>
          </el-form-item>

          <el-form-item label="用户名" prop="username">
            <div class="info-row static-info">
              {{ profileForm.username }}
            </div>
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <div class="info-row">
              <el-input
                v-model="profileForm.phone"
                placeholder="请输入手机号"
                size="default"
                :disabled="!isProfileEditing"
                :class="{ 'disabled-input': !isProfileEditing }"
              ></el-input>
            </div>
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <div class="info-row">
              <el-input
                v-model="profileForm.email"
                placeholder="请输入邮箱"
                type="email"
                size="default"
                :disabled="!isProfileEditing"
                :class="{ 'disabled-input': !isProfileEditing }"
              ></el-input>
            </div>
          </el-form-item>

          <!-- 性别选择 -->
          <el-form-item label="性别" prop="sex">
            <div class="info-row">
              <el-radio-group
                v-model="profileForm.sex"
                :disabled="!isProfileEditing"
                :class="{ 'disabled-input': !isProfileEditing }"
              >
                <el-radio label="1">男</el-radio>
                <el-radio label="0">女</el-radio>
                <el-radio label="3">未知</el-radio>
              </el-radio-group>
            </div>
          </el-form-item>

          <!-- 生日选择 - 修复非编辑模式显示问题 -->
          <el-form-item label="生日" prop="birthday">
            <div class="info-row">
              <!-- 非编辑模式显示文本，编辑模式显示选择器 -->
              <template v-if="!isProfileEditing">
                <div class="static-info">
                  {{ formatTimeDay(profileForm.birthday) || '' }}
                </div>
              </template>
              <template v-else>
                <el-date-picker
                  v-model="profileForm.birthday"
                  type="date"
                  placeholder="请选择生日"
                  size="default"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  :default-value="defaultBirthday"
                ></el-date-picker>
              </template>
            </div>
          </el-form-item>

          <el-form-item label="登录地址">
            <div class="info-row static-info">
              {{ profileForm.loginIp || '未知' }}
            </div>
          </el-form-item>

          <el-form-item label="登录时间">
            <div class="info-row static-info">
              {{ profileForm.loginTime ? formatTimeSecond(profileForm.loginTime) : '未知' }}
            </div>
          </el-form-item>

          <el-form-item label="个人简介">
            <div class="info-row">
              <el-input
                v-model="profileForm.bio"
                type="textarea"
                :rows="4"
                placeholder="请输入个人简介"
                size="default"
                :disabled="!isProfileEditing"
                :class="{ 'disabled-input': !isProfileEditing }"
              ></el-input>
            </div>
          </el-form-item>

          <el-form-item class="button-group-item">
            <div class="btn-group">
              <el-button
                @click="toggleProfileEdit"
                size="default"
                :class="isProfileEditing ? 'cancel-btn' : 'edit-btn'"
              >
                {{ isProfileEditing ? '取消' : '修改信息' }}
              </el-button>

              <el-button
                @click="saveProfile"
                size="default"
                class="save-btn"
                v-if="isProfileEditing"
              >
                保存
              </el-button>
            </div>
          </el-form-item>
        </el-form>

        <!-- 账号安全设置 -->
        <el-form
          v-if="activeTab === 'security'"
          ref="securityFormRef"
          :model="securityForm"
          :rules="securityRules"
          label-width="140px"
          class="setting-form-item"
        >
          <el-form-item label="当前密码" prop="currentPassword">
            <el-input
              v-model="securityForm.currentPassword"
              type="password"
              placeholder="请输入当前密码"
              size="default"
            ></el-input>
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="securityForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              size="default"
            ></el-input>
            <div v-if="securityForm.newPassword" class="password-strength-container">
              <div class="password-strength">
                <div class="strength-label">密码强度：</div>
                <div class="strength-bars">
                  <div class="strength-bar" :style="getPasswordStrengthStyle(1)"></div>
                  <div class="strength-bar" :style="getPasswordStrengthStyle(2)"></div>
                  <div class="strength-bar" :style="getPasswordStrengthStyle(3)"></div>
                </div>
                <div class="strength-text">{{ getPasswordStrengthText() }}</div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="securityForm.confirmPassword"
              type="password"
              placeholder="请确认新密码"
              size="default"
            ></el-input>
          </el-form-item>

          <el-form-item class="button-group-item">
            <div class="btn-group">
              <el-button
                @click="changePassword"
                size="default"
                class="save-btn"
                :loading="isPasswordLoading"
              >
                修改密码
              </el-button>

              <el-button @click="logout" size="default" class="logout-btn"> 退出登录 </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { useUserProfileStore } from '@/store/userProfile.js'
import { checkLogin, checkPhone } from '@/utils/commonUtils.js'
import { getImageDimensions } from '@/utils/fileUtils.js'
import { formatTimeDay, formatTimeSecond } from '@/utils/dateUtils.js'
import router from '@/router/index.js'
import { useHomeStatusStore } from '@/store/homeStatus.js'
import { useHistoryStore } from '@/store/history.js'
import { useCollectionStore } from '@/store/collection.js'
import { useChatStore } from '@/store/chat.js'

const userProfile = useUserProfileStore()
const homeStatus = useHomeStatusStore()
const history = useHistoryStore()
const collection = useCollectionStore()
const chat = useChatStore()

// 激活的标签页
const activeTab = ref('profile')

// 表单引用
const profileFormRef = ref(null)
const securityFormRef = ref(null)
const avatarRef = ref(null)
const avatarInputRef = ref(null)

// 编辑状态管理
const isProfileEditing = ref(false)
const isPasswordLoading = ref(false)

// 计算10年前的日期作为默认显示
const defaultBirthday = computed(() => {
  const date = new Date()
  date.setFullYear(date.getFullYear() - 10)
  return date
})

// 状态管理：新增头像上传状态
const isAvatarUploading = ref(false)

// 个人信息表单数据
const profileForm = reactive({
  avatar: userProfile.avatar,
  username: userProfile.username || '游客',
  email: userProfile.email || '',
  phone: userProfile.phone || '',
  sex: userProfile.sex ? userProfile.sex.toString() : '3',
  birthday: userProfile.birthday || null,
  loginIp: userProfile.lastLoginIp || '未知',
  loginTime: userProfile.lastLoginTime || null,
  bio: userProfile.bio || '',
})

// 安全设置表单数据
const securityForm = reactive({
  username: userProfile.username,
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// 个人信息表单验证规则
const profileRules = reactive({
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  phone: [{ validator: checkPhone, trigger: 'blur' }],
})

// 密码强度验证
const validatePasswordStrength = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度不能少于 6 个字符'))
  } else if (value.length < 8 || !/\d/.test(value) || !/[a-zA-Z]/.test(value)) {
    callback()
  } else if (
    value.length >= 8 &&
    /\d/.test(value) &&
    /[a-zA-Z]/.test(value) &&
    /[^a-zA-Z0-9]/.test(value)
  ) {
    callback() // 强密码
  } else {
    callback() // 中等强度密码
  }
}

// 确认密码验证
const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认新密码'))
  } else if (value !== securityForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 安全设置表单验证规则
const securityRules = reactive({
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' },
    { validator: validatePasswordStrength, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
})

// 获取密码强度
const getPasswordStrength = () => {
  const password = securityForm.newPassword
  if (!password) return 0

  let strength = 0
  // 长度>=6分1分，>=8分2分
  if (password.length >= 6) strength++
  if (password.length >= 8) strength++
  // 包含数字+字母（不区分大小写）则+1分
  if (/\d/.test(password) && /[a-zA-Z]/.test(password)) strength++

  return Math.min(strength, 3)
}

// 获取密码强度样式类
const getPasswordStrengthClass = (level) => {
  const strength = getPasswordStrength()
  if (strength === 0) return 'empty'
  return level <= strength
    ? strength === 1
      ? 'weak'
      : strength === 2
        ? 'medium'
        : 'strong'
    : 'empty'
}

watch(
  () => securityForm.newPassword,
  () => {
    // 无需额外逻辑，仅触发依赖更新
    getPasswordStrength()
  },
)

// 直接返回颜色样式，不依赖CSS类
const getPasswordStrengthStyle = (level) => {
  const strength = getPasswordStrength()
  // 定义颜色映射
  const colors = {
    empty: '#eee', // 空（灰色）
    weak: '#ff4d4f', // 弱（红色）
    medium: '#faad14', // 中（黄色）
    strong: '#52c41a', // 强（绿色）
  }
  // 根据强度和当前级别判断颜色
  if (strength === 0) {
    return { backgroundColor: colors.empty }
  }
  return {
    backgroundColor:
      level <= strength
        ? strength === 1
          ? colors.weak
          : strength === 2
            ? colors.medium
            : colors.strong
        : colors.empty,
  }
}

// 获取密码强度文本
const getPasswordStrengthText = () => {
  const strength = getPasswordStrength()
  switch (strength) {
    case 0:
      return ''
    case 1:
      return '弱'
    case 2:
      return '中'
    case 3:
      return '强'
    default:
      return ''
  }
}

// 切换菜单
const handleMenuSelect = (key) => {
  activeTab.value = key
  resetForms()
}

// 重置所有表单
const resetForms = () => {
  isProfileEditing.value = false
  profileFormRef.value?.resetFields()
  securityFormRef.value?.resetFields()
}

// 触发头像上传
const triggerAvatarUpload = () => {
  if (isProfileEditing.value) {
    avatarInputRef.value.click()
  }
}

// 处理头像变更
const handleAvatarChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  // 验证文件类型和大小（保持原有）
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB')
    return
  }

  // 上传中状态
  isAvatarUploading.value = true
  try {
    // 构建FormData
    const formData = new FormData()
    formData.append('multipartFile', file)
    const { width, height } = await getImageDimensions(file)
    const avatarFile = {
      fileName: file.name,
      fileSize: file.size,
      userId: userProfile.userId,
      sessionId: 0,
      isImage: file.type.startsWith('image/') ? 1 : 0,
      imageWidth: width,
      imageHeight: height,
    }
    formData.append('fileJson', JSON.stringify(avatarFile))

    // 调用头像上传接口
    const response = await request('post', '/update/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    if (response.code === 200 && response.data?.filePath) {
      // 上传成功，更新头像URL
      profileForm.avatar = response.data.filePath
      userProfile.avatar = response.data.filePath
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(response.msg || '头像上传失败')
    }
  } catch (error) {
    console.error('头像上传异常:', error)
    ElMessage.error('网络异常，头像上传失败')
  } finally {
    isAvatarUploading.value = false
    e.target.value = ''
  }
}

// 切换个人信息编辑状态
const toggleProfileEdit = () => {
  if (isProfileEditing.value) {
    // 取消编辑，恢复原始数据
    profileFormRef.value.clearValidate()
    profileForm.avatar = userProfile.avatar
    profileForm.username = userProfile.username
    profileForm.phone = userProfile.phone
    profileForm.email = userProfile.email
    profileForm.sex = userProfile.sex !== undefined ? userProfile.sex.toString() : '3'
    profileForm.birthday = userProfile.birthday
    profileForm.loginIp = userProfile.lastLoginIp
    profileForm.loginTime = userProfile.lastLoginTime
    profileForm.bio = userProfile.bio
  }
  isProfileEditing.value = !isProfileEditing.value
}

// 保存个人信息
const saveProfile = () => {
  profileFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 构造与后端匹配的请求数据（类型转换）
        // 处理birthday格式：仅当是纯日期（不含T）时才添加时间部分
        let birthdayValue = profileForm.birthday
        if (birthdayValue && !birthdayValue.includes('T')) {
          // 纯日期格式（yyyy-MM-dd），补充时间
          birthdayValue = `${birthdayValue}T00:00:00`
        }
        const requestData = {
          userId: userProfile.userId,
          username: userProfile.username,
          phone: profileForm.phone,
          email: profileForm.email,
          avatar: profileForm.avatar,
          bio: profileForm.bio,
          sex: Number(profileForm.sex),
          birthday: birthdayValue || null,
        }

        const response = await request('post', '/update/info', requestData)
        if (response.code === 200) {
          ElMessage.success('修改成功')
          // 更新用户存储（同步类型）
          userProfile.setUserProfile(requestData)
          userProfile.token = response.data.token
          isProfileEditing.value = false
        } else {
          ElMessage.error(response.msg || '修改失败')
        }
      } catch (error) {
        console.error('修改个人信息失败:', error)
        ElMessage.error('网络异常，修改失败')
      }
    } else {
      ElMessage.warning('请完善信息后再保存')
      return false
    }
  })
}

// 修改密码
const changePassword = () => {
  securityFormRef.value.validate(async (valid) => {
    if (valid) {
      isPasswordLoading.value = true
      try {
        const response = await request('post', '/update/pwd', {
          userId: userProfile.userId,
          username: securityForm.username,
          oldPwd: securityForm.currentPassword,
          newPwd: securityForm.newPassword,
        })

        if (response.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          securityFormRef.value.resetFields()
          userProfile.clearUserProfile()
          isPasswordLoading.value = false
          setTimeout(() => {
            router.push('/login')
          }, 500)
        } else {
          ElMessage.error(response.msg || '密码修改失败')
        }
      } catch (error) {
        console.error('修改密码失败', error)
      } finally {
        isPasswordLoading.value = false
      }
    }
  })
}

const logout = () => {
  userProfile.clearUserProfile()
  homeStatus.clearHomeStatus()
  history.clearHistory()
  collection.clearCollection()
  chat.clearChat()
  ElMessage.success('已退出登录')
  setTimeout(() => {
    window.open('/home', '_blank')
  }, 300)
}

watch(
  () => [
    userProfile.avatar,
    userProfile.username,
    userProfile.phone,
    userProfile.email,
    userProfile.sex,
    userProfile.birthday,
    userProfile.bio,
  ],
  ([avatar, username, phone, email, sex, birthday, bio]) => {
    profileForm.avatar = avatar
    profileForm.username = username
    profileForm.phone = phone
    profileForm.email = email
    profileForm.sex = sex ? sex.toString() : '3'
    profileForm.birthday = birthday
    profileForm.bio = bio
  },
  { immediate: true },
)

// 初始化
onMounted(() => {
  if (!checkLogin(userProfile)) {
    router.push('/login')
  }
})
</script>

<style>
/* 密码强度条全局样式 */
/* 强度条容器：与输入框保持8px间距，确保在输入框正下方 */
.password-strength-container {
  margin-top: 8px;
  width: 100%;
  box-sizing: border-box;
  padding-left: 0;
}

/* 强度条整体布局：横向排列，不换行 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

/* 强度标签：固定宽度，避免抖动 */
.strength-label {
  width: 60px;
  text-align: left;
}

/* 强度条容器：占据中间主要空间 */
.strength-bars {
  flex: 1;
  max-width: 180px;
  display: flex;
  gap: 4px;
  height: 6px;
}

/* 单个强度条：均匀分配宽度，圆角 */
.strength-bar {
  flex: 1;
  border-radius: 3px;
  height: 100%;
  transition: background-color 0.3s ease;
}

/* 强度文本：固定宽度，右对齐 */
.strength-text {
  width: 40px;
  text-align: left;
}
</style>

<style scoped>
/* 输入框 */
::v-deep .el-input__inner,
::v-deep .el-textarea__inner {
  height: 32px !important;
  font-size: 13px !important;
  padding: 0 12px !important;
  line-height: 32px !important;
}

::v-deep .el-textarea__inner {
  min-height: 80px !important;
  line-height: 1.5 !important;
}

/* 调整按钮大小 */
::v-deep .el-button--default {
  padding: 5px 16px !important;
  font-size: 13px !important;
  height: 32px !important;
  line-height: 32px !important;
}

/* 生日选择器样式优化 */
::v-deep .el-date-picker {
  --el-datepicker-cell-height: 30px;
}

/* 静态信息样式与输入框保持一致 */
.static-info {
  font-size: 13px !important;
  height: 32px !important;
  line-height: 32px !important;
  padding: 0 12px !important;
  background: #f5f7fa;
  border-radius: 8px;
  width: 100%;
  box-sizing: border-box;
}

/* 其他原有样式保持不变 */
::v-deep .el-radio-group {
  display: flex;
  gap: 20px;
  padding: 5px 0;
}

::v-deep .el-radio {
  font-size: 13px;
}

::v-deep .el-date-editor {
  width: 100% !important;
}

::v-deep .disabled-input .el-radio {
  color: #666;
  cursor: not-allowed;
}

.profile-container {
  width: 100%;
  min-height: 100vh;
  background-image: url('@/assets/images/login_bg.jpg');
  background-size: cover;
  padding: 30px 20px;
  box-sizing: border-box;
}

.profile-header {
  padding-bottom: 15px;
  text-align: left;
  max-width: 80%;
  margin: 0 auto 30px;
}

.profile-header h1 {
  font-size: 28px;
  margin-bottom: 12px;
  color: #333;
  font-weight: 500;
}

.profile-content {
  display: flex;
  gap: 30px;
  align-items: flex-start;
  max-width: 70%;
  margin: 0 auto;
}

.profile-menu {
  width: 220px;
  min-width: 220px;
  border-radius: 12px;
  background: #f5f7fa;
  border: 1px solid #e5e6eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 10px 0;
  font-size: 14px;
}

::v-deep .profile-menu .el-menu-item {
  color: #333;
  height: 50px;
  line-height: 50px;
  margin: 0 5px;
  border-radius: 8px;
}

::v-deep .profile-menu .el-menu-item.is-active {
  background-color: #e6f7ff;
  color: #1890ff;
}

::v-deep .profile-menu .el-menu-item:hover {
  background-color: #f0f2f5;
}

.profile-form {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e6eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 20px 40px;
  margin-top: 0;
  max-width: 60%;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  box-sizing: border-box;
}

.setting-form-item {
  width: 100%;
  max-width: 600px;
}

.profile-form::-webkit-scrollbar {
  width: 6px;
}

.profile-form::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 3px;
}

.profile-form::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.profile-form::-webkit-scrollbar-thumb:hover {
  background: #bbb;
}

::v-deep .el-form-item {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  width: 80%;
}

::v-deep .el-form-item__label {
  color: #333;
  font-weight: normal;
  font-size: 13px;
  padding-right: 15px;
}

::v-deep .el-form-item__content {
  flex: 1;
  padding: 0 !important;
  margin: 0 !important;
}

.info-row {
  display: flex;
  align-items: center;
  width: 100%;
}

::v-deep .el-input,
::v-deep .el-input__wrapper,
::v-deep .el-textarea {
  border-radius: 8px !important;
  width: 100% !important;
}

::v-deep .disabled-input .el-input__inner,
::v-deep .disabled-input .el-textarea__inner {
  border-radius: 8px !important;
  background-color: #f5f7fa !important;
  cursor: not-allowed;
  width: 100% !important;
}

::v-deep .el-input__inner::placeholder,
::v-deep .el-textarea__inner::placeholder {
  color: #999 !important;
  font-size: 13px;
}

.avatar-container {
  display: flex;
  align-items: center;
}

.avatar-wrapper {
  position: relative;
  width: 50px;
  height: 50px;
}

/* 头像预览容器 */
.avatar-preview {
  position: relative;
  width: 50px;
  height: 50px;
}

/* 自定义加载动画容器 */
.custom-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #1890ff;
  border-radius: 50%;
  /* 旋转动画 */
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 头像上传提示 */
.avatar-tip {
  margin-left: 15px;
  font-size: 12px;
  color: #999;
  line-height: 50px;
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #ddd;
  background-color: #f5f7fa;
}

.avatar-input {
  display: none;
}

.avatar-upload-overlay {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.button-group-item {
  justify-content: center !important;
  margin-top: 30px !important;
  margin-bottom: 10px !important;
  width: 100% !important;
}

.btn-group {
  display: flex;
  gap: 15px;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.edit-btn,
.save-btn,
.cancel-btn,
.logout-btn {
  border-radius: 8px !important;
  font-size: 13px !important;
  padding: 5px 16px !important;
  transition: all 0.2s ease;
}

.edit-btn,
.save-btn,
.logout-btn {
  color: #1890ff;
  border: 1px solid #1890ff;
  background-color: transparent;
}

.edit-btn:hover,
.save-btn:hover,
.logout-btn:hover {
  background-color: #e6f7ff;
}

.cancel-btn {
  color: #666;
  border: 1px solid #ddd;
  background-color: transparent;
}

.cancel-btn:hover {
  background-color: #f5f7fa;
}

::v-deep .el-form-item__error {
  color: #ff4d4f;
  font-size: 12px;
}
</style>
