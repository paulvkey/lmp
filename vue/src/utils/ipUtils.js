import axios from 'axios'

// 获取公网IP
export const getPublicIp = async () => {
  try {
    const res = await axios.get('https://api.ipify.org?format=json');
    return res.data.ip;
  } catch (error) {
    console.error('获取IP失败：', error);
    return '未知IP';
  }
};

// 获取带地域信息的IP（使用ip-api，免费版有请求频率限制）
export const getIpWithLocation = async () => {
  try {
    const res = await axios.get('http://ip-api.com/json');
    return res.data;
    // 返回示例：{ "query": "xxx.xxx.xxx.xxx", "country": "中国", "city": "北京", ... }
  } catch (error) {
    console.error('获取IP地域信息失败：', error);
    return null;
  }
};
