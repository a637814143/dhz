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

export interface ProductOverview {
  totalProducts: number
  onSaleProducts: number
  offSaleProducts: number
  soldOutProducts: number
  totalStock: number
  totalSalesVolume: number
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

export interface HomepageBanner {
  id: string
  title: string
  description?: string | null
  imageUrl?: string | null
  linkUrl?: string | null
  ctaText?: string | null
}

export interface HomepagePromotion {
  id: string
  title: string
  description: string
  discountRate?: number | null
  tags?: string[]
  startDate?: string | null
  endDate?: string | null
}

export interface HomepageAnnouncement {
  id: string
  title: string
  content: string
  type: string
  publishedAt: string
  linkUrl?: string | null
}

export interface HomepageContent {
  banners: HomepageBanner[]
  recommendedProducts: ProductSummary[]
  hotProducts: ProductSummary[]
  promotions: HomepagePromotion[]
  announcements: HomepageAnnouncement[]
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
