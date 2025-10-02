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
