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
  unit?: string | null
  stock: number
  sales: number
  mainImage?: string | null
  status: string
  createdAt: string
  categoryName?: string | null
  supplierName?: string | null
  supplierLevel?: string | null
}

export interface ProductImage {
  id: number
  imageUrl: string
  sortOrder?: number | null
  createdAt?: string | null
}

export interface ProductSizeAllocation {
  id?: number
  sizeLabel: string
  quantity: number
}

export interface ProductDetail extends ProductSummary {
  updatedAt?: string | null
  images?: ProductImage[] | null
  sizeAllocations?: ProductSizeAllocation[] | null
  category?: {
    id: number
    name: string
    description?: string | null
  } | null
  supplier?: {
    id: number
    companyName: string
    supplierLevel?: string | null
    contactName?: string | null
    contactPhone?: string | null
  } | null
}

export interface CartItemProduct {
  id: number
  name: string
  mainImage?: string | null
  price: number
  status?: string | null
}

export interface CartItem {
  id: number
  quantity: number
  unitPrice: number
  subtotal: number
  addedAt?: string | null
  product: CartItemProduct
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
  consumerLookupId?: string
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
  consumerLookupId?: string
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
  paymentMethod?: string | null
  shippingAddress?: string | null
  recipientName?: string | null
  recipientPhone?: string | null
  orderTime: string
  paymentTime?: string | null
  shippingTime?: string | null
  deliveryTime?: string | null
  inTransitTime?: string | null
  consumerConfirmationTime?: string | null
  adminApprovalTime?: string | null
  payoutStatus?: string | null
  adminHoldingAmount?: number | null
  managingAdminId?: number | null
  managingAdminName?: string | null
  orderItems: OrderItemDetail[]
}

export interface ConsumerAddress {
  id: number
  consumerId?: number | null
  recipientName: string
  recipientPhone: string
  shippingAddress: string
  isDefault: boolean
  createdAt: string
  updatedAt: string
}

export interface ConsumerAddressPayload {
  recipientName: string
  recipientPhone: string
  shippingAddress: string
  isDefault?: boolean
}

export interface SupplierOrderItem {
  id: number
  quantity: number
  unitPrice: number
  totalPrice: number
  createdAt: string
  productId?: number | null
  productName?: string | null
  productMainImage?: string | null
}

export interface SupplierOrderSummary {
  id: number
  orderNo: string
  status: string
  totalQuantity: number
  totalAmount: number
  supplierTotalQuantity: number
  supplierTotalAmount: number
  recipientName?: string | null
  recipientPhone?: string | null
  shippingAddress?: string | null
  orderTime: string
  paymentTime?: string | null
  shippingTime?: string | null
  deliveryTime?: string | null
  inTransitTime?: string | null
  canShip: boolean
  mixedSuppliers: boolean
  items: SupplierOrderItem[]
}

export interface AdminOrderItem {
  id: number
  quantity: number
  unitPrice: number
  totalPrice: number
  productId?: number | null
  productName?: string | null
  productMainImage?: string | null
  supplierId?: number | null
  supplierName?: string | null
}

export interface AdminOrderSummary {
  id: number
  orderNo: string
  totalAmount: number
  totalQuantity: number
  status: string
  receiptStatus: string
  consumerConfirmed: boolean
  cancelled: boolean
  cancellationLabel?: string | null
  commissionAmount: number
  supplierPayoutAmount: number
  payoutStatus?: string | null
  adminHoldingAmount?: number | null
  managingAdminName?: string | null
  consumerName?: string | null
  recipientName?: string | null
  recipientPhone?: string | null
  shippingAddress?: string | null
  orderTime: string
  paymentTime?: string | null
  shippingTime?: string | null
  deliveryTime?: string | null
  inTransitTime?: string | null
  consumerConfirmationTime?: string | null
  adminApprovalTime?: string | null
  items: AdminOrderItem[]
}

export interface ProductReview {
  id: number
  orderId: number
  orderItemId: number
  productId: number
  productName: string
  consumerId?: number | null
  consumerName?: string | null
  authorId?: number | null
  authorName?: string | null
  authorRole?: string | null
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
  adminResolution?: string | null
  requestedAt: string
  processedAt?: string | null
  adminProcessedAt?: string | null
  afterReceipt?: boolean | null
  requiresAdminApproval?: boolean | null
  adminStatus?: string | null
  refundAmount?: number | null
  supplierShareAmount?: number | null
  commissionAmount?: number | null
}

export interface CaptchaChallenge {
  challengeId: string
  question: string
  expiresIn: number
}

export type UserRole = 'consumer' | 'supplier' | 'admin'
export type RegisterableUserRole = 'consumer' | 'supplier'

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
  userType: RegisterableUserRole
}
