export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface ProductSummary {
  id: number
  name: string
  description?: string | null
  price: number
  stock: number
  sales: number
  mainImage?: string | null
  status: string
  createdAt: string
  categoryName?: string | null
  supplierName?: string | null
  supplierLevel?: string | null
}

export interface PurchaseOrderItemPayload {
  product: {
    id: number
  }
  quantity: number
}

export interface PurchaseOrderPayload {
  consumer: {
    id: number
  }
  recipientName: string
  recipientPhone: string
  shippingAddress: string
  paymentMethod?: string | null
  remark?: string | null
  orderItems: PurchaseOrderItemPayload[]
}

export interface PurchaseOrderResult {
  id: number
  orderNo: string
  status: string
  totalAmount: number
  totalQuantity: number
  paymentMethod?: string | null
  shippingAddress?: string | null
  recipientName?: string | null
  recipientPhone?: string | null
}

export interface ProductOverview {
  totalProducts: number
  onSaleProducts: number
  offSaleProducts: number
  soldOutProducts: number
  totalStock: number
  totalSalesVolume: number
}

export interface Banner {
  imageUrl: string
  headline: string
  subHeadline: string
  ctaText: string
  targetUrl: string
}

export interface Promotion {
  productId: number
  title: string
  description: string
  discountRate: number
  validUntil: string
}

export interface Announcement {
  id: string
  title: string
  content: string
  category: string
  publishedAt: string
}

export interface NewsItem {
  id: string
  title: string
  summary: string
  source: string
  publishedAt: string
}

export interface HomepageContent {
  recommendations: ProductSummary[]
  hotSales: ProductSummary[]
  promotions: Promotion[]
  banners: Banner[]
  announcements: Announcement[]
  news: NewsItem[]
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface CategoryOption {
  id: number
  name: string
}

export interface SupplierOption {
  id: number
  companyName: string
  supplierLevel?: string | null
}

export interface OrderItemDetail {
  id: number
  quantity: number
  unitPrice: number
  totalPrice: number
  createdAt: string
  product: {
    id: number
    name: string
    mainImage?: string | null
  }
}

export interface OrderDetail {
  id: number
  orderNo: string
  totalAmount: number
  totalQuantity: number
  status: string
  shippingAddress?: string | null
  recipientName?: string | null
  recipientPhone?: string | null
  orderTime: string
  paymentTime?: string | null
  shippingTime?: string | null
  deliveryTime?: string | null
  orderItems: OrderItemDetail[]
}

export interface ProductReview {
  id: number
  orderId: number
  orderItemId: number
  productId: number
  productName: string
  consumerId: number
  consumerName: string
  rating: number
  comment?: string | null
  createdAt: string
}

export interface ReturnRequest {
  id: number
  orderId: number
  orderItemId: number
  productId: number
  productName: string
  consumerId: number
  consumerName: string
  status: string
  reason?: string | null
  resolution?: string | null
  requestedAt: string
  processedAt?: string | null
}

export interface CaptchaChallenge {
  challengeId: string
  question: string
  expiresIn: number
}

export type UserRole = 'consumer' | 'supplier' | 'admin'

export interface AuthUser {
  id: number
  username: string
  email?: string | null
  phone?: string | null
  userType: UserRole
}

export interface LoginPayload {
  username: string
  password: string
  challengeId: string
  verificationCode: string
}

export interface LoginResponse {
  token: string
  expiresIn: number
  issuedAt: number
  redirectUrl: string
  user: AuthUser
}

export interface RegisterPayload {
  username: string
  password: string
  confirmPassword: string
  email: string
  phone: string
  userType: UserRole
}
