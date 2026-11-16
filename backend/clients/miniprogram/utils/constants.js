/**
 * 线上洗车平台 v6.0 · 业务常量（小程序端）
 * 【金额口径】后端一律以「分」传输；本文件所有 *Fen 字段均为分。
 *            前端展示用 fen2Yuan() 转换。下列价格为示例值，非最终定价。
 * 【支付口径】V1 测试版模拟支付，成功文案为橙色「模拟支付成功」，严禁绿色「支付成功」。
 */

const BASE_URL = '/api/';

// 统一响应体 { code, msg, data }：code 为这些值表示业务成功（兼容 0/200）
const SUCCESS_CODES = [0, 200];

// 服务三档（到店价；价格单位：分，示例值）
const SERVICE_TIERS = [
  {
    key: 'basic', name: '外观清洁', short: '外观洗',
    priceFen: 8300, desc: '高压冲洗 · 中性泡沫 · 轮毂清洁',
    feature: ['高压冲洗车身', '中性泡沫去污', '轮毂清洁', '外观擦干'],
    recommend: false
  },
  {
    key: 'standard', name: '标准清洁', short: '标准洗',
    priceFen: 13300, desc: '外观清洁 · 内饰吸尘 · 中控擦拭',
    feature: ['含外观清洁全部项目', '内饰吸尘', '中控台擦拭', '车窗清洁'],
    recommend: true
  },
  {
    key: 'premium', name: '精细清洁', short: '精细洗',
    priceFen: 24300, desc: '标准清洁 · 深度去污 · 打蜡养护',
    feature: ['含标准清洁全部项目', '缝隙深度刷洗', '漆面去污', '打蜡养护'],
    recommend: false
  }
];

// 上门服务价（单位：分，示例值）。仅外观洗/标准洗支持上门；精细清洁请选到店。
const DOOR_PRICE_FEN = {
  basic: 9500,
  standard: 15500
};

// 增值项目（单位：分，示例值，最多选 4 项）
const ADDONS = [
  { key: 'wax', name: '漆面打蜡', priceFen: 3000 },
  { key: 'interior', name: '内饰深度清洁', priceFen: 5000 },
  { key: 'disinfect', name: '车内消毒', priceFen: 2000 },
  { key: 'tire', name: '轮胎养护', priceFen: 1500 }
];
const MAX_ADDONS = 4;

// 保险：L1 强制锁定不可取消；L2 可选（单位：分，示例值）
const INSURANCE = {
  L1: {
    key: 'L1', name: 'L1 平台强制保障', priceFen: 0,
    required: true, cancelable: false, desc: '平台强制保障，不可取消'
  },
  L2: {
    key: 'L2', name: 'L2 增值保险', priceFen: 1200,
    required: false, cancelable: true, desc: '可选投保（示例价 ¥12）'
  }
};

// 洗车工资质认证：C1/C2/C3 服务等级 + D 驾驶资格 + O 操作规范
const CERTS = {
  C1: { key: 'C1', name: '基础洗车认证', allow: 'basic', desc: '可承接外观清洁' },
  C2: { key: 'C2', name: '标准洗车认证', allow: 'standard', desc: '可承接标准清洁' },
  C3: { key: 'C3', name: '精细洗车认证', allow: 'premium', desc: '需在线考试 ≥ 80 分，可承接精细清洁' },
  D:  { key: 'D',  name: '驾驶资格认证', allow: 'pickup', desc: '可承接取送车任务' },
  O:  { key: 'O',  name: '操作规范认证', allow: 'bay', desc: '可操作高压设备与工位' }
};

// 订单 8 态 Stepper
const ORDER_STATUS = [
  { key: 'unpaid', label: '待支付', tone: 'warning' },
  { key: 'accepted_pending', label: '待接单', tone: 'info' },
  { key: 'accepted', label: '已接单', tone: 'info' },
  { key: 'picking', label: '取车中', tone: 'primary' },
  { key: 'washing', label: '洗车中', tone: 'primary' },
  { key: 'delivering', label: '配送中', tone: 'primary' },
  { key: 'verifying', label: '待验收', tone: 'warning' },
  { key: 'done', label: '完成', tone: 'success' }
];

// 工位三态
const BAY_STATUS = [
  { key: 'free', label: '有空位', color: '#18A058' },
  { key: 'full', label: '已满', color: '#E5484D' },
  { key: 'paused', label: '暂停', color: '#97A3B6' }
];

// 五维评价（低分 ≤2 星强制填写原因）
const REVIEW_DIMS = [
  { key: 'attitude', label: '服务态度' },
  { key: 'quality', label: '清洁质量' },
  { key: 'punctual', label: '准时交付' },
  { key: 'protect', label: '车辆保护' },
  { key: 'communicate', label: '沟通响应' }
];
const LOW_SCORE = 2;

// 模拟支付红线文案
const SIM_BANNER_TEXT = '测试环境 · 模拟支付 · 不产生真实资金收付';
const SIM_SUCCESS_TEXT = '模拟支付成功';
const SIM_SUCCESS_FULL = '模拟支付成功 · 测试数据，不产生真实资金收付';

// 金额：分 → 元（展示字符串，保留两位）
function fen2Yuan(fen) {
  const n = Number(fen || 0);
  return (n / 100).toFixed(2);
}
// 金额：元 → 分（提交后端）
function yuan2Fen(yuan) {
  return Math.round(Number(yuan || 0) * 100);
}

module.exports = {
  BASE_URL,
  SUCCESS_CODES,
  SERVICE_TIERS,
  DOOR_PRICE_FEN,
  ADDONS,
  MAX_ADDONS,
  INSURANCE,
  CERTS,
  ORDER_STATUS,
  BAY_STATUS,
  REVIEW_DIMS,
  LOW_SCORE,
  SIM_BANNER_TEXT,
  SIM_SUCCESS_TEXT,
  SIM_SUCCESS_FULL,
  fen2Yuan,
  yuan2Fen
};
