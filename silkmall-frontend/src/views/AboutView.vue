<template>
  <section class="about">
    <header>
      <h1>关于 SilkMall</h1>
      <p>一体化的蚕制品销售与供应链协同平台</p>
    </header>

    <div class="content">
      <article>
        <h2>数字化蚕桑产业链</h2>
        <p>
          SilkMall 基于 Spring Boot 与 Vue 技术栈构建，聚焦蚕制品的选品管理、库存调度、渠道销售与供应商协作。
          后端提供完整的产品、分类、订单与权限管理 API，并通过可扩展的数据模型支撑多角色协同；前端以数据可视化和交互式筛选为核心，帮助业务人员快速洞察市场动态。
        </p>
      </article>

      <article>
        <h2>功能亮点</h2>
        <ul>
          <li>多维筛选与排序：支持按分类、供应商、价格区间、状态等条件精准定位商品。</li>
          <li>运营概览驾驶舱：实时呈现商品数量、库存、销量等关键指标。</li>
          <li>角色友好的界面：针对采购、运营和销售人员定制信息架构和展示重点。</li>
          <li>可持续扩展：以微服务友好的结构设计，便于接入支付、物流、BI 等外部能力。</li>
        </ul>
      </article>

      <article>
        <h2>技术栈</h2>
        <div class="tech">
          <div>
            <h3>后端</h3>
            <p>Spring Boot · Spring Data JPA · Spring Security · MySQL</p>
          </div>
          <div>
            <h3>前端</h3>
            <p>Vue 3 · TypeScript · Vite · Axios</p>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.about {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
  padding: 2rem;
  border-radius: 2rem;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.1);
}

header h1 {
  font-size: clamp(2rem, 3vw, 2.6rem);
  font-weight: 800;
  margin-bottom: 0.75rem;
}

header p {
  color: rgba(0, 0, 0, 0.6);
}

.content {
  display: grid;
  gap: 2rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

article {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

article h2 {
  font-size: 1.25rem;
  font-weight: 700;
}

article p,
article li {
  color: rgba(0, 0, 0, 0.75);
  line-height: 1.7;
}

article ul {
  padding-left: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.tech {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.tech h3 {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

@media (prefers-color-scheme: dark) {
  .about {
    background: rgba(31, 41, 55, 0.85);
    box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
  }

  header p,
  article p,
  article li {
    color: rgba(226, 232, 240, 0.8);
  }
}
</style>